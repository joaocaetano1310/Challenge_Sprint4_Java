package com.futurevet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/** Tradução dos erros lançados pelos pacotes PL/SQL (Sprint 4 de Banco de Dados). */
class ErroBancoExceptionTest {

    @Test
    void erroDeNegocioDoPacoteViraBadRequestComMensagemLimpa() {
        var e = ErroBancoException.de(new SQLException(
                "ORA-20004: CPF invalido: os digitos verificadores nao conferem.\n"
                        + "ORA-06512: at \"RM562074.PKG_FV_UTIL\", line 60",
                "72000", 20004));
        assertThat(e.getStatus()).isEqualTo(400);
        assertThat(e.getCodigoOracle()).isEqualTo("ORA-20004");
        assertThat(e.getMessage()).isEqualTo("CPF invalido: os digitos verificadores nao conferem.");
    }

    @Test
    void duplicidadeEIntegridadeViramConflito() {
        assertThat(ErroBancoException.de(new SQLException("ORA-20005: Ja existe usuario", "72000", 20005))
                .getStatus()).isEqualTo(409);
        assertThat(ErroBancoException.de(new SQLException("ORA-20008: Possui historico", "72000", 20008))
                .getStatus()).isEqualTo(409);
    }

    @Test
    void registroInexistenteViraNotFound() {
        assertThat(ErroBancoException.de(new SQLException("ORA-20006: Animal 9 nao encontrado.", "72000", 20006))
                .getStatus()).isEqualTo(404);
    }

    @Test
    void erroInesperadoViraErroInterno() {
        var e = ErroBancoException.de(new SQLException("ORA-20099: Erro inesperado", "72000", 20099));
        assertThat(e.getStatus()).isEqualTo(500);
        assertThat(e.getMessage()).isEqualTo("Erro inesperado");
    }
}
