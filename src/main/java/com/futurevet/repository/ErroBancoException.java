package com.futurevet.repository;

import java.sql.SQLException;

/**
 * Erro devolvido pelos pacotes PL/SQL (faixa ORA-20001 a ORA-20099) já traduzido para HTTP.
 * Os pacotes registram o erro em TB_LOG_ERRO antes de lançá-lo; aqui só convertemos o código.
 */
public class ErroBancoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int status;
    private final String codigoOracle;

    public ErroBancoException(int status, String codigoOracle, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
        this.codigoOracle = codigoOracle;
    }

    public int getStatus() {
        return status;
    }

    public String getCodigoOracle() {
        return codigoOracle;
    }

    /** Converte a SQLException do driver Oracle em um erro com status HTTP e mensagem de negócio. */
    public static ErroBancoException de(SQLException e) {
        int codigo = e.getErrorCode(); // ORA-20004 chega como 20004
        String codigoOracle = codigo > 0 ? String.format("ORA-%05d", codigo) : "SQL-" + e.getSQLState();
        int status;
        if (codigo == 20005 || codigo == 20008) {
            status = 409; // duplicidade ou integridade
        } else if (codigo == 20006 || codigo == 20009) {
            status = 404; // registro não encontrado / sem dados
        } else if (codigo >= 20001 && codigo <= 20010) {
            status = 400; // regra de negócio ou dado inválido
        } else {
            status = 500;
        }
        return new ErroBancoException(status, codigoOracle, primeiraLinha(e.getMessage(), codigo), e);
    }

    /** "ORA-20004: CPF invalido...\nORA-06512: at ..." vira "CPF invalido...". */
    private static String primeiraLinha(String mensagem, int codigo) {
        if (mensagem == null || mensagem.isBlank()) {
            return "Erro no banco de dados.";
        }
        String linha = mensagem.lines().findFirst().orElse(mensagem).strip();
        String prefixo = String.format("ORA-%05d: ", codigo);
        if (linha.startsWith(prefixo)) {
            linha = linha.substring(prefixo.length());
        }
        return linha;
    }
}
