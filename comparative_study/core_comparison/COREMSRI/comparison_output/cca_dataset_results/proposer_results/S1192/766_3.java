```java
package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;

import java.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoAvulsaTest {

    private static final String CNJP_INVALID_13 = "1234567890123";
    private static final String CNJP_VALID_14 = "12345678901234";
    private static final String PHONE_VALID_8 = "81579357";
    private static final String MATRICULA_LONG = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM";
    private static final String MATRICULA_VERY_LONG = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final String NOME_AGENTE_LONG = "lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL";
    private static final String NOME_AGENTE_VERY_LONG = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final String NUMERO_DOC_LONG = "qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi";
    private static final String NUMERO_DOC_VERY_LONG = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final String ORGAO_EMITENTE_LONG = "qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV";
    private static final String ORGAO_EMITENTE_VERY_LONG = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final String REPARTICAO_FISCAL_LONG = "YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3";
    private static final String REPARTICAO_FISCAL_VERY_LONG = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final BigDecimal VALOR_TOTAL_VALID = new BigDecimal("999999999999.99");
    private static final BigDecimal VALOR_TOTAL_INVALID = new BigDecimal("10000000000000");

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCnpjComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setCnpj(CNJP_INVALID_13);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setCnpj(CNJP_VALID_14);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirFoneComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setFone("12345");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setFone(MATRICULA_VERY_LONG);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirMatriculaAgenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setMatriculaAgente("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setMatriculaAgente(MATRICULA_VERY_LONG);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNomeAgenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setNomeAgente("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setNomeAgente(NOME_AGENTE_VERY_LONG);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroDocumentoArrecadacaoReceitaComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setNumeroDocumentoArrecadacaoReceita("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setNumeroDocumentoArrecadacaoReceita(NUMERO_DOC_VERY_LONG);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirOrgaoEmitenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setOrgaoEmitente("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setOrgaoEmitente(ORGAO_EMITENTE_VERY_LONG);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirReparticaoFiscalEmitenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setReparticaoFiscalEmitente("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setReparticaoFiscalEmitente(REPARTICAO_FISCAL_VERY_LONG);
        }
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorTotalConstanteDocumentoArrecadacaoReceitaComTamanhoInvalido() {
        new NFNotaInfoAvulsa().setValorTotalConstanteDocumentoArrecadacaoReceita(VALOR_TOTAL_INVALID);
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCnpjNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(PHONE_VALID_8);
        avulsa.setMatriculaAgente(MATRICULA_LONG);
        avulsa.setNomeAgente(NOME_AGENTE_LONG);
        avulsa.setNumeroDocumentoArrecadacaoReceita(NUMERO_DOC_LONG);
        avulsa.setOrgaoEmitente(ORGAO_EMITENTE_LONG);
        avulsa.setReparticaoFiscalEmitente(REPARTICAO_FISCAL_LONG);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(VALOR_TOTAL_VALID);
        avulsa.toString();
    }

    @Test
    public void devePermitirDataEmissaoDocumentoArrecadacaoNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(CNJP_VALID_14);
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(PHONE_VALID_8);
        avulsa.setMatriculaAgente(MATRICULA_LONG);
        avulsa.setNomeAgente(NOME_AGENTE_LONG);
        avulsa.setNumeroDocumentoArrecadacaoReceita(NUMERO_DOC_LONG);
        avulsa.setOrgaoEmitente(ORGAO_EMITENTE_LONG);
        avulsa.setReparticaoFiscalEmitente(REPARTICAO_FISCAL_LONG);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(VALOR_TOTAL_VALID);
        avulsa.toString();
    }

    @Test
    public void devePermitirDataPagamentoDocumentoArrecadacaoNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(CNJP_VALID_14);
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setFone(PHONE_VALID_8);
        avulsa.setMatriculaAgente(MATRICULA_LONG);
        avulsa.setNomeAgente(NOME_AGENTE_LONG);
        avulsa.setNumeroDocumentoArrecadacaoReceita(NUMERO_DOC_LONG);
        avulsa.setOrgaoEmitente(ORGAO_EMITENTE_LONG);
        avulsa.setReparticaoFiscalEmitente(REPARTICAO_FISCAL_LONG);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(VALOR_TOTAL_VALID);
        avulsa.toString();
    }

    @Test
    public void devePermitirFoneNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(CNJP_VALID_14);
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setMatriculaAgente(MATRICULA_LONG);
        avulsa.setNomeAgente(NOME_AGENTE_LONG);
        avulsa.setNumeroDocumentoArrecadacaoReceita(NUMERO_DOC_LONG);
        avulsa.setOrgaoEmitente(ORGAO_EMITENTE_LONG);
        avulsa.setReparticaoFiscalEmitente(REPARTICAO_FISCAL_LONG);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(VALOR_TOTAL_VALID);
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirMatriculaAgenteNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(CNJP_VALID_14);
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(PHONE_VALID_8);
        avulsa.setNomeAgente(NOME_AGENTE_LONG);
        avulsa.setNumeroDocumentoArrecadacaoReceita(NUMERO_DOC_LONG);
        avulsa.setOrgaoEmitente(ORGAO_EMITENTE_LONG);
        avulsa.setReparticaoFiscalEmitente(REPARTICAO_FISCAL_LONG);

        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNomeAgenteNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone("81579357");
        avulsa.setMatriculaAgente("Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM");
        avulsa.setNumeroDocumentoArrecadacaoReceita("qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi");
        avulsa.setOrgaoEmitente("qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV");
        avulsa.setReparticaoFiscalEmitente("YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3");
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test
    public void devePermitirNumeroDocumentoArrecadacaoReceitaNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone("81579357");
        avulsa.setMatriculaAgente("Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM");
        avulsa.setNomeAgente("lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL");
        avulsa.setOrgaoEmitente("qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV");
        avulsa.setReparticaoFiscalEmitente("YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3");
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirOrgaoEmitenteNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone("81579357");
        avulsa.setMatriculaAgente("Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM");
        avulsa.setNomeAgente("lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL");
        avulsa.setNumeroDocumentoArrecadacaoReceita("qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi");
        avulsa.setReparticaoFiscalEmitente("YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3");
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirReparticaoFiscalEmitenteNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone("81579357");
        avulsa.setMatriculaAgente("Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM");
        avulsa.setNomeAgente("lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL");
        avulsa.setNumeroDocumentoArrecadacaoReceita("qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi");
        avulsa.setOrgaoEmitente("qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV");
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirUFNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone("81579357");
        avulsa.setMatriculaAgente("Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM");
        avulsa.setNomeAgente("lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL");
        avulsa.setNumeroDocumentoArrecadacaoReceita("qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi");
        avulsa.setOrgaoEmitente("qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV");
        avulsa.setReparticaoFiscalEmitente("YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3");
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test
    public void devePermitirValorTotalConstanteDocumentoArrecadacaoReceitaNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone("81579357");
        avulsa.setMatriculaAgente("Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM");
        avulsa.setNomeAgente("lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL");
        avulsa.setNumeroDocumentoArrecadacaoReceita("qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi");
        avulsa.setOrgaoEmitente("qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV");
        avulsa.setReparticaoFiscalEmitente("YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3");
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.toString();
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        final String xmlEsperado = "<NFNotaInfoAvulsa><CNPJ>12345678901234</CNPJ><xOrgao>qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV</xOrgao><matr>Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM</matr><xAgente>lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL</xAgente><fone>81579357</fone><UF>RS</UF><nDAR>qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi</nDAR><dEmi>2014-01-13</dEmi><vDAR>999999999999.99</vDAR><repEmi>YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3</repEmi><dPag>2014-03-21</dPag></NFNotaInfoAvulsa>";
        Assert.assertEquals(xmlEsperado, FabricaDeObjetosFake.getNFNotaInfoAvulsa().toString());
    }
}