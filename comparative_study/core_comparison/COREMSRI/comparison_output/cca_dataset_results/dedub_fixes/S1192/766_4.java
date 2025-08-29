package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;

import java.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoAvulsaTest {

    private static final String INVALID_LONG_STRING = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final String VALID_LONG_STRING = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM";
    private static final String VALID_LONG_STRING_2 = "lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL";
    private static final String VALID_LONG_STRING_3 = "qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi";
    private static final String VALID_LONG_STRING_4 = "qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV";
    private static final String VALID_LONG_STRING_5 = "YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3";
    private static final String PHONE_NUMBER = "81579357";
    private static final String EMPTY_STRING = "";

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCnpjComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setCnpj("1234567890123");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setCnpj("123456789012345");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirFoneComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setFone("12345");
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setFone("123456789012345");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirMatriculaAgenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setMatriculaAgente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setMatriculaAgente(INVALID_LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNomeAgenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setNomeAgente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setNomeAgente(INVALID_LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroDocumentoArrecadacaoReceitaComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setNumeroDocumentoArrecadacaoReceita(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setNumeroDocumentoArrecadacaoReceita(INVALID_LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirOrgaoEmitenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setOrgaoEmitente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setOrgaoEmitente(INVALID_LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirReparticaoFiscalEmitenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setReparticaoFiscalEmitente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setReparticaoFiscalEmitente(INVALID_LONG_STRING);
        }
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorTotalConstanteDocumentoArrecadacaoReceitaComTamanhoInvalido() {
        new NFNotaInfoAvulsa().setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("10000000000000"));
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCnpjNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(PHONE_NUMBER);
        avulsa.setMatriculaAgente(VALID_LONG_STRING);
        avulsa.setNomeAgente(VALID_LONG_STRING_2);
        avulsa.setNumeroDocumentoArrecadacaoReceita(VALID_LONG_STRING_3);
        avulsa.setOrgaoEmitente(VALID_LONG_STRING_4);
        avulsa.setReparticaoFiscalEmitente(VALID_LONG_STRING_5);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test
    public void devePermitirDataEmissaoDocumentoArrecadacaoNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(PHONE_NUMBER);
        avulsa.setMatriculaAgente(VALID_LONG_STRING);
        avulsa.setNomeAgente(VALID_LONG_STRING_2);
        avulsa.setNumeroDocumentoArrecadacaoReceita(VALID_LONG_STRING_3);
        avulsa.setOrgaoEmitente(VALID_LONG_STRING_4);
        avulsa.setReparticaoFiscalEmitente(VALID_LONG_STRING_5);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test
    public void devePermitirDataPagamentoDocumentoArrecadacaoNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setFone(PHONE_NUMBER);
        avulsa.setMatriculaAgente(VALID_LONG_STRING);
        avulsa.setNomeAgente(VALID_LONG_STRING_2);
        avulsa.setNumeroDocumentoArrecadacaoReceita(VALID_LONG_STRING_3);
        avulsa.setOrgaoEmitente(VALID_LONG_STRING_4);
        avulsa.setReparticaoFiscalEmitente(VALID_LONG_STRING_5);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test
    public void devePermitirFoneNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setMatriculaAgente(VALID_LONG_STRING);
        avulsa.setNomeAgente(VALID_LONG_STRING_2);
        avulsa.setNumeroDocumentoArrecadacaoReceita(VALID_LONG_STRING_3);
        avulsa.setOrgaoEmitente(VALID_LONG_STRING_4);
        avulsa.setReparticaoFiscalEmitente(VALID_LONG_STRING_5);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(new BigDecimal("999999999999.99"));
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirMatriculaAgenteNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj("12345678901234");
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(PHONE_NUMBER);
        avulsa.setNomeAgente(VALID_LONG_STRING_2);
        avulsa.setNumeroDocumentoArrecadacaoReceita(VALID_LONG_STRING_3);
        avulsa.setOrgaoEmitente(VALID_LONG_STRING_4);
        avulsa.setReparticaoFiscalEmitente(VALID_LONG_STRING_5);

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