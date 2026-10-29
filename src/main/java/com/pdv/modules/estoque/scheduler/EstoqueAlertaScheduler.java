package com.pdv.modules.estoque.scheduler;

import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EstoqueAlertaScheduler {

    private final ProdutoRepository produtoRepo;
    private final TenantRepository  tenantRepo;
    private final JavaMailSender    mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Scheduled(cron = "${app.estoque.alerta.cron:0 0 7 * * MON,FRI}")
    @Transactional(readOnly = true)
    public void verificarEstoqueCritico() {
        log.info("[EstoqueAlerta] Iniciando verificação diária de estoque crítico...");
        tenantRepo.findAllAtivos().forEach(t -> {
            try { processarTenant(t); }
            catch (Exception ex) {
                log.error("[EstoqueAlerta] Erro no tenant {}: {}", t.getId(), ex.getMessage(), ex);
            }
        });
        log.info("[EstoqueAlerta] Verificação concluída.");
    }

    private void processarTenant(Tenant tenant) throws Exception {
        List<Produto> criticos = produtoRepo
                .findByTenantIdAndAtivoTrueAndEstoqueAbaixoMinimo(tenant.getId());
        if (criticos.isEmpty()) return;

        String dest = tenant.getEmail();
        if (dest == null || dest.isBlank()) {
            log.warn("[EstoqueAlerta] Tenant {} sem e-mail. Pulando.", tenant.getNome());
            return;
        }

        log.info("[EstoqueAlerta] {} — {} produto(s) crítico(s) → {}",
                tenant.getNome(), criticos.size(), dest);
        enviarEmail(tenant, dest, criticos);
    }

    private void enviarEmail(Tenant tenant, String dest, List<Produto> criticos) throws Exception {
        var msg    = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(remetente, "PDV SaaS — Alertas");
        helper.setTo(dest);
        helper.setSubject("⚠️ Estoque Mínimo — " + tenant.getNome()
                + " — " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        helper.setText(montarHtml(tenant, criticos), true);
        mailSender.send(msg);
    }

    private String montarHtml(Tenant tenant, List<Produto> produtos) {
        // Agrupa por categoria para facilitar leitura
        Map<String, List<Produto>> porCategoria = produtos.stream()
                .collect(Collectors.groupingBy(p ->
                        p.getCategoria() != null ? p.getCategoria().getNome() : "Sem categoria"));

        var sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"/><style>
            body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px}
            .wrap{max-width:660px;margin:0 auto;background:#fff;border-radius:10px;
                  overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,.12)}
            .hdr{background:#1e3a5f;color:#fff;padding:24px 28px}
            .hdr h1{margin:0;font-size:20px;font-weight:700}
            .hdr p{margin:6px 0 0;font-size:13px;opacity:.75}
            .body{padding:24px 28px}
            .resumo{background:#fff3cd;border:1px solid #ffc107;border-radius:7px;
                    padding:12px 16px;margin-bottom:22px;font-size:14px;color:#856404}
            .categ{font-size:12px;font-weight:700;color:#1e3a5f;text-transform:uppercase;
                   letter-spacing:.07em;margin:22px 0 8px;padding-bottom:5px;
                   border-bottom:2px solid #e2e8f0}
            table{width:100%;border-collapse:collapse;font-size:13px}
            th{background:#f8fafc;color:#64748b;font-weight:600;text-align:left;
               padding:9px 10px;border-bottom:1px solid #e2e8f0}
            td{padding:9px 10px;border-bottom:1px solid #f1f5f9;vertical-align:top}
            .estoque{color:#dc2626;font-weight:700}
            .minimo{color:#94a3b8;font-size:11px}
            .forn{font-size:12px;color:#334155;margin-top:3px}
            .forn-nome{font-weight:600;color:#1e3a5f}
            .wa-btn{display:inline-block;margin-top:4px;padding:3px 10px;
                    background:#25D366;color:#fff;border-radius:5px;font-size:11px;
                    font-weight:700;text-decoration:none}
            .no-forn{font-size:11px;color:#94a3b8;font-style:italic}
            .footer{background:#f8fafc;padding:14px 28px;font-size:11px;
                    color:#94a3b8;text-align:center}
            </style></head><body><div class="wrap">
            """);

        sb.append(String.format("""
            <div class="hdr">
              <h1>⚠️ Alerta de Estoque Mínimo</h1>
              <p>%s — %s</p>
            </div>
            """,
            tenant.getNome(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"))));

        sb.append(String.format("""
            <div class="body">
            <div class="resumo">
              <strong>%d produto(s)</strong> estão com estoque igual ou abaixo do mínimo.
              Verifique os fornecedores indicados e realize a reposição.
            </div>
            """, produtos.size()));

        porCategoria.forEach((cat, prods) -> {
            sb.append(String.format("<div class=\"categ\">%s</div>", cat));
            sb.append("""
                <table>
                  <thead><tr>
                    <th>Produto</th>
                    <th style="text-align:right">Atual</th>
                    <th style="text-align:right">Mínimo</th>
                    <th>Fornecedor</th>
                  </tr></thead><tbody>
                """);

            for (Produto p : prods) {
                BigDecimal diff = p.getEstoqueAtual().subtract(p.getEstoqueMinimo());

                // Bloco do fornecedor
                String fornecedorHtml;
                if (p.getFornecedor() != null) {
                    var f    = p.getFornecedor();
                    var nome = f.getNomeFantasia() != null && !f.getNomeFantasia().isBlank()
                               ? f.getNomeFantasia() : f.getRazaoSocial();
                    var wa   = montarWhatsappUrl(f.getTelefone());

                    fornecedorHtml = String.format(
                        "<div class=\"forn\"><span class=\"forn-nome\">%s</span>", nome);
                    if (f.getTelefone() != null && !f.getTelefone().isBlank()) {
                        fornecedorHtml += String.format(
                            "<br/><span style=\"color:#64748b\">%s</span>", f.getTelefone());
                    }
                    if (wa != null) {
                        fornecedorHtml += String.format(
                            "<br/><a href=\"%s\" class=\"wa-btn\">💬 WhatsApp</a>", wa);
                    }
                    fornecedorHtml += "</div>";
                } else {
                    fornecedorHtml = "<span class=\"no-forn\">Não vinculado</span>";
                }

                sb.append(String.format("""
                    <tr>
                      <td>%s<div class="minimo">%s</div></td>
                      <td style="text-align:right" class="estoque">%s %s</td>
                      <td style="text-align:right" class="minimo">%s %s</td>
                      <td>%s</td>
                    </tr>
                    """,
                    p.getNome(),
                    p.getUnidade(),
                    p.getEstoqueAtual().stripTrailingZeros().toPlainString(), p.getUnidade(),
                    p.getEstoqueMinimo().stripTrailingZeros().toPlainString(), p.getUnidade(),
                    fornecedorHtml));
            }
            sb.append("</tbody></table>");
        });

        sb.append("""
            </div>
            <div class="footer">
              PDV SaaS — Alerta automático enviado diariamente às 07h.<br/>
              Acesse o sistema para ajustar o estoque ou vincular fornecedores.
            </div>
            </div></body></html>
            """);

        return sb.toString();
    }

    /** Converte telefone brasileiro (com ou sem DDI) para URL do WhatsApp */
    private String montarWhatsappUrl(String telefone) {
        if (telefone == null || telefone.isBlank()) return null;
        String digits = telefone.replaceAll("\\D", "");
        if (!digits.startsWith("55") && digits.length() >= 10) digits = "55" + digits;
        return "https://wa.me/" + digits;
    }
}
