package com.futurevet.web.api;

import com.futurevet.repository.ErroBancoException;
import com.futurevet.repository.ProcedimentosRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprint 4 (Banco de Dados): endpoints que executam os pacotes PL/SQL do Oracle.
 *
 * <p>Fica em {@code /api/admin/**}, então exige login JWT com perfil ADMIN (clínica). As regras de
 * negócio e as mensagens de erro vêm do próprio banco; o Java só traduz o código ORA-20xxx para HTTP.
 */
@RestController
@RequestMapping("/api/admin/banco")
@Profile("oracle")
public class BancoApiController {
    private static final MediaType JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);
    private static final String ARQUIVO_EXPORTACAO = "2TDSPF_2026_Dataset_FutureVet.json";

    private final ProcedimentosRepository banco;
    private final PasswordEncoder encoder;

    public BancoApiController(ProcedimentosRepository banco, PasswordEncoder encoder) {
        this.banco = banco;
        this.encoder = encoder;
    }

    public record TutorRequest(String nome, String email, String senha, Long cpf, Long telefone) {
    }

    public record AnimalRequest(Long idUsuario, String nome, String especie, String raca, String idade,
            String porte, BigDecimal peso) {
    }

    public record PesoRequest(BigDecimal peso) {
    }

    public record VacinaRequest(Long idAnimal, String nome, LocalDate data, LocalDate proximaDose, String local,
            Long idDoseOrigem) {
    }

    public record ConsultaRequest(Long idAnimal, String tipo, LocalDate data, String hora, String local,
            String observacao) {
    }

    public record StatusRequest(String status, String observacao) {
    }

    // ------------------------------------------------------------------ cadastros (PKG_FV_CADASTRO)

    @PostMapping("/tutores")
    public ResponseEntity<Map<String, Object>> cadastrarTutor(@RequestBody TutorRequest r) {
        banco.exigirSenhaForte(r.senha()); // regra da senha validada no banco (PKG_FV_VALIDACAO)
        String hash = encoder.encode(r.senha()); // o banco só recebe o hash BCrypt
        long id = banco.cadastrarTutor(r.nome(), r.email(), hash, r.cpf(), r.telefone());
        return criado("PKG_FV_CADASTRO.PRC_CADASTRAR_TUTOR", "idUsuario", id);
    }

    @PostMapping("/animais")
    public ResponseEntity<Map<String, Object>> cadastrarAnimal(@RequestBody AnimalRequest r) {
        long id = banco.cadastrarAnimal(r.idUsuario(), r.nome(), r.especie(), r.raca(), r.idade(), r.porte(),
                r.peso());
        return criado("PKG_FV_CADASTRO.PRC_CADASTRAR_ANIMAL", "idAnimal", id);
    }

    @PatchMapping("/animais/{id}/peso")
    public Map<String, Object> atualizarPeso(@PathVariable long id, @RequestBody PesoRequest r) {
        banco.atualizarPeso(id, r.peso());
        return resposta("PKG_FV_CADASTRO.PRC_ATUALIZAR_PESO", "idAnimal", id);
    }

    @DeleteMapping("/animais/{id}")
    public Map<String, Object> excluirAnimal(@PathVariable long id) {
        banco.excluirAnimal(id);
        return resposta("PKG_FV_CADASTRO.PRC_EXCLUIR_ANIMAL", "idAnimal", id);
    }

    @PostMapping("/vacinas")
    public ResponseEntity<Map<String, Object>> registrarVacina(@RequestBody VacinaRequest r) {
        long id = banco.registrarVacina(r.idAnimal(), r.nome(), r.data(), r.proximaDose(), r.local(),
                r.idDoseOrigem());
        return criado("PKG_FV_CADASTRO.PRC_REGISTRAR_VACINA", "idVacina", id);
    }

    @PostMapping("/consultas")
    public ResponseEntity<Map<String, Object>> agendarConsulta(@RequestBody ConsultaRequest r) {
        long id = banco.agendarConsulta(r.idAnimal(), r.tipo(), r.data(), r.hora(), r.local(), r.observacao());
        return criado("PKG_FV_CADASTRO.PRC_AGENDAR_CONSULTA", "idConsulta", id);
    }

    @PatchMapping("/consultas/{id}/status")
    public Map<String, Object> alterarStatus(@PathVariable long id, @RequestBody StatusRequest r) {
        banco.alterarStatusConsulta(id, r.status(), r.observacao());
        return resposta("PKG_FV_CADASTRO.PRC_ALTERAR_STATUS_CONSULTA", "idConsulta", id);
    }

    // ------------------------------------------------------------------ consultas e relatórios

    @GetMapping("/validacoes/cpf/{cpf}")
    public Map<String, Object> validarCpf(@PathVariable long cpf) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("funcao", "PKG_FV_VALIDACAO.FNC_VALIDAR_CPF");
        corpo.put("cpf", cpf);
        corpo.put("valido", banco.cpfValido(cpf));
        return corpo;
    }

    @GetMapping("/relatorios/especies")
    public List<Map<String, Object>> resumoPorEspecie() {
        return banco.resumoPorEspecie();
    }

    @GetMapping("/relatorios/vacinas-pendentes")
    public List<Map<String, Object>> vacinasPendentes(@RequestParam(defaultValue = "30") int dias) {
        return banco.vacinasPendentes(dias);
    }

    @GetMapping("/relatorios/animais-json")
    public ResponseEntity<String> animaisJson() {
        return ResponseEntity.ok().contentType(JSON_UTF8).body(banco.animaisJson());
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> auditoria(@RequestParam(required = false) String tabela) {
        return banco.auditoria(tabela);
    }

    // ------------------------------------------------------------------ exportação para o MongoDB

    /** Gera o JSON pelo PKG_FV_EXPORTACAO, salva em ./export e devolve o conteúdo. */
    @GetMapping("/exportar-json")
    public ResponseEntity<String> exportarJson() throws IOException {
        String json = banco.exportarDataset();
        Path pasta = Path.of("export");
        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve(ARQUIVO_EXPORTACAO).toAbsolutePath();
        Files.writeString(arquivo, json, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("X-Arquivo-Exportado", arquivo.toString())
                .contentType(JSON_UTF8)
                .body(json);
    }

    // ------------------------------------------------------------------ erros

    @ExceptionHandler(ErroBancoException.class)
    public ResponseEntity<Map<String, Object>> erroBanco(ErroBancoException e) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", e.getStatus());
        corpo.put("codigoOracle", e.getCodigoOracle());
        corpo.put("mensagem", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(corpo);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> erroArquivo(IOException e) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", 500);
        corpo.put("mensagem", "Não foi possível salvar o arquivo exportado: " + e.getMessage());
        return ResponseEntity.status(500).body(corpo);
    }

    private static ResponseEntity<Map<String, Object>> criado(String procedure, String campoId, long id) {
        return ResponseEntity.status(201).body(resposta(procedure, campoId, id));
    }

    private static Map<String, Object> resposta(String procedure, String campoId, long id) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("procedure", procedure);
        corpo.put(campoId, id);
        corpo.put("mensagem", "Executado com sucesso no Oracle.");
        return corpo;
    }
}
