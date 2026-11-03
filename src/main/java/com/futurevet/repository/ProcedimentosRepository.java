package com.futurevet.repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Sprint 4 (Banco de Dados): chamadas aos pacotes PL/SQL do Oracle via JDBC (CallableStatement).
 *
 * <p>Usa as mesmas tabelas e sequences (FV_SEQ_*) do restante da aplicação. Cada chamada roda com
 * autocommit (padrão do pool Hikari): se a procedure terminar sem erro, a gravação é confirmada; se
 * lançar ORA-20xxx, ela mesma desfaz o trabalho (SAVEPOINT) e o erro vira {@link ErroBancoException}.
 *
 * <p>Só existe no perfil {@code oracle}: o perfil {@code demo} usa H2, que não tem os pacotes.
 */
@Repository
@Profile("oracle")
public class ProcedimentosRepository {

    /** OracleTypes.CURSOR. O valor fica aqui porque o driver Oracle é dependência só de runtime. */
    private static final int CURSOR_ORACLE = -10;

    private final DataSource dataSource;

    public ProcedimentosRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @FunctionalInterface
    private interface Chamada<T> {
        T executar(Connection conexao) throws SQLException;
    }

    /** Abre a conexão, executa e fecha; SQLException vira ErroBancoException. */
    private <T> T executar(Chamada<T> chamada) {
        try (Connection conexao = dataSource.getConnection()) {
            return chamada.executar(conexao);
        } catch (SQLException e) {
            throw ErroBancoException.de(e);
        }
    }

    // ------------------------------------------------------------------ PKG_FV_VALIDACAO

    /** PKG_FV_VALIDACAO.PRC_EXIGIR_SENHA_FORTE: lança ORA-20002 se a senha for fraca. */
    public void exigirSenhaForte(String senha) {
        executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_VALIDACAO.PRC_EXIGIR_SENHA_FORTE(?)}")) {
                cs.setString(1, senha);
                cs.execute();
                return null;
            }
        });
    }

    /** PKG_FV_VALIDACAO.FNC_VALIDAR_CPF: 1 = válido, 0 = inválido. */
    public boolean cpfValido(long cpf) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall("{? = call PKG_FV_VALIDACAO.FNC_VALIDAR_CPF(?)}")) {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.setLong(2, cpf);
                cs.execute();
                return cs.getInt(1) == 1;
            }
        });
    }

    // ------------------------------------------------------------------ PKG_FV_CADASTRO

    /** PKG_FV_CADASTRO.PRC_CADASTRAR_TUTOR: a senha chega já em hash BCrypt. Devolve o ID gerado. */
    public long cadastrarTutor(String nome, String email, String senhaHash, Long cpf, Long telefone) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_CADASTRO.PRC_CADASTRAR_TUTOR(?, ?, ?, ?, ?, ?)}")) {
                cs.setString(1, nome);
                cs.setString(2, email);
                cs.setString(3, senhaHash);
                numero(cs, 4, cpf);
                numero(cs, 5, telefone);
                cs.registerOutParameter(6, Types.NUMERIC);
                cs.execute();
                return cs.getLong(6);
            }
        });
    }

    /** PKG_FV_CADASTRO.PRC_CADASTRAR_ANIMAL. Devolve o ID gerado. */
    public long cadastrarAnimal(Long idUsuario, String nome, String especie, String raca, String idade,
            String porte, BigDecimal peso) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall(
                    "{call PKG_FV_CADASTRO.PRC_CADASTRAR_ANIMAL(?, ?, ?, ?, ?, ?, ?, ?)}")) {
                numero(cs, 1, idUsuario);
                cs.setString(2, nome);
                cs.setString(3, especie);
                cs.setString(4, raca);
                cs.setString(5, idade);
                cs.setString(6, porte);
                decimal(cs, 7, peso);
                cs.registerOutParameter(8, Types.NUMERIC);
                cs.execute();
                return cs.getLong(8);
            }
        });
    }

    /** PKG_FV_CADASTRO.PRC_ATUALIZAR_PESO (o gatilho grava o UPDATE em TB_AUDITORIA). */
    public void atualizarPeso(long idAnimal, BigDecimal peso) {
        executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_CADASTRO.PRC_ATUALIZAR_PESO(?, ?)}")) {
                cs.setLong(1, idAnimal);
                decimal(cs, 2, peso);
                cs.execute();
                return null;
            }
        });
    }

    /** PKG_FV_CADASTRO.PRC_EXCLUIR_ANIMAL: recusa (ORA-20008) se houver histórico clínico. */
    public void excluirAnimal(long idAnimal) {
        executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_CADASTRO.PRC_EXCLUIR_ANIMAL(?)}")) {
                cs.setLong(1, idAnimal);
                cs.execute();
                return null;
            }
        });
    }

    /** PKG_FV_CADASTRO.PRC_REGISTRAR_VACINA (idDoseOrigem preenchido = reforço). Devolve o ID gerado. */
    public long registrarVacina(Long idAnimal, String nome, LocalDate data, LocalDate proximaDose, String local,
            Long idDoseOrigem) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall(
                    "{call PKG_FV_CADASTRO.PRC_REGISTRAR_VACINA(?, ?, ?, ?, ?, ?, ?)}")) {
                numero(cs, 1, idAnimal);
                cs.setString(2, nome);
                data(cs, 3, data);
                data(cs, 4, proximaDose);
                cs.setString(5, local);
                numero(cs, 6, idDoseOrigem);
                cs.registerOutParameter(7, Types.NUMERIC);
                cs.execute();
                return cs.getLong(7);
            }
        });
    }

    /** PKG_FV_CADASTRO.PRC_AGENDAR_CONSULTA (verifica conflito de horário no banco). Devolve o ID gerado. */
    public long agendarConsulta(Long idAnimal, String tipo, LocalDate data, String hora, String local,
            String observacao) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall(
                    "{call PKG_FV_CADASTRO.PRC_AGENDAR_CONSULTA(?, ?, ?, ?, ?, ?, ?)}")) {
                numero(cs, 1, idAnimal);
                cs.setString(2, tipo);
                data(cs, 3, data);
                cs.setString(4, hora);
                cs.setString(5, local);
                cs.setString(6, observacao);
                cs.registerOutParameter(7, Types.NUMERIC);
                cs.execute();
                return cs.getLong(7);
            }
        });
    }

    /** PKG_FV_CADASTRO.PRC_ALTERAR_STATUS_CONSULTA (CONFIRMADA, CANCELADA ou REALIZADA). */
    public void alterarStatusConsulta(long idConsulta, String status, String observacao) {
        executar(c -> {
            try (CallableStatement cs = c.prepareCall(
                    "{call PKG_FV_CADASTRO.PRC_ALTERAR_STATUS_CONSULTA(?, ?, ?)}")) {
                cs.setLong(1, idConsulta);
                cs.setString(2, status);
                cs.setString(3, observacao);
                cs.execute();
                return null;
            }
        });
    }

    // ------------------------------------------------------------------ PKG_FV_RELATORIO / AUDITORIA

    /** PKG_FV_RELATORIO.PRC_RESUMO_ESPECIE (cursor). */
    public List<Map<String, Object>> resumoPorEspecie() {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_RELATORIO.PRC_RESUMO_ESPECIE(?)}")) {
                cs.registerOutParameter(1, CURSOR_ORACLE);
                cs.execute();
                try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                    return linhas(rs);
                }
            }
        });
    }

    /** PKG_FV_RELATORIO.PRC_VACINAS_PENDENTES (cursor). */
    public List<Map<String, Object>> vacinasPendentes(int dias) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_RELATORIO.PRC_VACINAS_PENDENTES(?, ?)}")) {
                cs.setInt(1, dias);
                cs.registerOutParameter(2, CURSOR_ORACLE);
                cs.execute();
                try (ResultSet rs = (ResultSet) cs.getObject(2)) {
                    return linhas(rs);
                }
            }
        });
    }

    /** PKG_FV_AUDITORIA.PRC_LISTAR (cursor) - trilha gravada pelos gatilhos. */
    public List<Map<String, Object>> auditoria(String tabela) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall("{call PKG_FV_AUDITORIA.PRC_LISTAR(?, ?)}")) {
                cs.setString(1, tabela);
                cs.registerOutParameter(2, CURSOR_ORACLE);
                cs.execute();
                try (ResultSet rs = (ResultSet) cs.getObject(2)) {
                    return linhas(rs);
                }
            }
        });
    }

    /** PKG_FV_RELATORIO.FNC_LISTAR_ANIMAIS_JSON (função da Sprint 3, JSON montado no banco). */
    public String animaisJson() {
        return clob("{? = call PKG_FV_RELATORIO.FNC_LISTAR_ANIMAIS_JSON}");
    }

    // ------------------------------------------------------------------ PKG_FV_EXPORTACAO

    /** PKG_FV_EXPORTACAO.FNC_EXPORTAR_DATASET: dataset completo em JSON para o MongoDB. */
    public String exportarDataset() {
        return clob("{? = call PKG_FV_EXPORTACAO.FNC_EXPORTAR_DATASET}");
    }

    // ------------------------------------------------------------------ auxiliares

    private String clob(String chamada) {
        return executar(c -> {
            try (CallableStatement cs = c.prepareCall(chamada)) {
                cs.registerOutParameter(1, Types.CLOB);
                cs.execute();
                Clob clob = cs.getClob(1);
                if (clob == null) {
                    return null;
                }
                try {
                    return clob.getSubString(1, (int) clob.length());
                } finally {
                    clob.free();
                }
            }
        });
    }

    private static void numero(CallableStatement cs, int indice, Long valor) throws SQLException {
        if (valor == null) {
            cs.setNull(indice, Types.NUMERIC);
        } else {
            cs.setLong(indice, valor);
        }
    }

    private static void decimal(CallableStatement cs, int indice, BigDecimal valor) throws SQLException {
        if (valor == null) {
            cs.setNull(indice, Types.NUMERIC);
        } else {
            cs.setBigDecimal(indice, valor);
        }
    }

    private static void data(CallableStatement cs, int indice, LocalDate valor) throws SQLException {
        if (valor == null) {
            cs.setNull(indice, Types.DATE);
        } else {
            cs.setDate(indice, java.sql.Date.valueOf(valor));
        }
    }

    /** Converte o cursor em lista de mapas (coluna em minúsculas -> valor) para virar JSON. */
    private static List<Map<String, Object>> linhas(ResultSet rs) throws SQLException {
        List<Map<String, Object>> resultado = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                linha.put(meta.getColumnLabel(i).toLowerCase(Locale.ROOT), valor(rs.getObject(i)));
            }
            resultado.add(linha);
        }
        return resultado;
    }

    private static Object valor(Object valor) throws SQLException {
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime().toString();
        }
        if (valor instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (valor instanceof Clob clob) {
            return clob.getSubString(1, (int) clob.length());
        }
        if (valor != null && valor.getClass().getName().startsWith("oracle.")) {
            return valor.toString(); // tipos proprietários do driver
        }
        return valor;
    }
}
