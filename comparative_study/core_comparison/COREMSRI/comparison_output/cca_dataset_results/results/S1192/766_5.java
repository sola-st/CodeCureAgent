package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;

import java.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoAvulsaTest {

    private static final String INVALID_LENGTH_CNPJ = "1234567890123";
    private static final String VALID_LENGTH_CNPJ = "123456789012345";
    private static final String INVALID_LENGTH_FONE = "12345";
    private static final String VALID_LENGTH_FONE = "123456789012345";
    private static final String EMPTY_STRING = "";
    private static final String LONG_STRING = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM1";
    private static final String LONG_STRING_64 = "Nn5PPREBbkfmmk4lBFwgvkuKg8prnY5CPqHIzqGiD1lTnZJ37nAZ4NBc8XwM";
    private static final String LONG_STRING_60 = "lkLip3hIYSAIzH3Tf1LWQsaybqB76V66lMgWBcHVwcOKInuJ8mGUyY8DT4NL";
    private static final String LONG_STRING_56 = "qqDt1f1ulcahrBnUH0otPFkjYqD2tH4ktYsR71WSYZLFW1zZObAqajHHkyxi";
    private static final String LONG_STRING_64_ALT1 = "qNre0x2eJthUYIoKBuBbbGSeA4R2wrDLxNwCuDFkYD54flBLbBBMakGDgQUV";
    private static final String LONG_STRING_64_ALT2 = "YQFmDI2HBjjfZpRjR2ghwmSo1oWk5QgUEYf2oG46uEHwY4zsXyH1ORSr8oq3";
    private static final String VALID_FONE = "81579357";
    private static final BigDecimal BIG_DECIMAL_10000000000000 = new BigDecimal("10000000000000");
    private static final BigDecimal BIG_DECIMAL_999999999999_99 = new BigDecimal("999999999999.99");

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCnpjComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setCnpj(INVALID_LENGTH_CNPJ);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setCnpj(VALID_LENGTH_CNPJ);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirFoneComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setFone(INVALID_LENGTH_FONE);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setFone(VALID_LENGTH_FONE);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirMatriculaAgenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setMatriculaAgente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setMatriculaAgente(LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNomeAgenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setNomeAgente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setNomeAgente(LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroDocumentoArrecadacaoReceitaComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setNumeroDocumentoArrecadacaoReceita(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setNumeroDocumentoArrecadacaoReceita(LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirOrgaoEmitenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setOrgaoEmitente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setOrgaoEmitente(LONG_STRING);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirReparticaoFiscalEmitenteComTamanhoInvalido() {
        try {
            new NFNotaInfoAvulsa().setReparticaoFiscalEmitente(EMPTY_STRING);
        } catch (final IllegalStateException e) {
            new NFNotaInfoAvulsa().setReparticaoFiscalEmitente(LONG_STRING);
        }
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorTotalConstanteDocumentoArrecadacaoReceitaComTamanhoInvalido() {
        new NFNotaInfoAvulsa().setValorTotalConstanteDocumentoArrecadacaoReceita(BIG_DECIMAL_10000000000000);
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCnpjNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(VALID_FONE);
        avulsa.setMatriculaAgente(LONG_STRING_64_ALT1);
        avulsa.setNomeAgente(LONG_STRING_60);
        avulsa.setNumeroDocumentoArrecadacaoReceita(LONG_STRING_56);
        avulsa.setOrgaoEmitente(LONG_STRING_64_ALT1);
        avulsa.setReparticaoFiscalEmitente(LONG_STRING_64_ALT2);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(BIG_DECIMAL_999999999999_99);
        avulsa.toString();
    }

    @Test
    public void devePermitirDataEmissaoDocumentoArrecadacaoNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(VALID_LENGTH_CNPJ);
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(VALID_FONE);
        avulsa.setMatriculaAgente(LONG_STRING_64_ALT1);
        avulsa.setNomeAgente(LONG_STRING_60);
        avulsa.setNumeroDocumentoArrecadacaoReceita(LONG_STRING_56);
        avulsa.setOrgaoEmitente(LONG_STRING_64_ALT1);
        avulsa.setReparticaoFiscalEmitente(LONG_STRING_64_ALT2);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(BIG_DECIMAL_999999999999_99);
        avulsa.toString();
    }

    @Test
    public void devePermitirDataPagamentoDocumentoArrecadacaoNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(VALID_LENGTH_CNPJ);
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setFone(VALID_FONE);
        avulsa.setMatriculaAgente(LONG_STRING_64_ALT1);
        avulsa.setNomeAgente(LONG_STRING_60);
        avulsa.setNumeroDocumentoArrecadacaoReceita(LONG_STRING_56);
        avulsa.setOrgaoEmitente(LONG_STRING_64_ALT1);
        avulsa.setReparticaoFiscalEmitente(LONG_STRING_64_ALT2);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(BIG_DECIMAL_999999999999_99);
        avulsa.toString();
    }

    @Test
    public void devePermitirFoneNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(VALID_LENGTH_CNPJ);
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setMatriculaAgente(LONG_STRING_64_ALT1);
        avulsa.setNomeAgente(LONG_STRING_60);
        avulsa.setNumeroDocumentoArrecadacaoReceita(LONG_STRING_56);
        avulsa.setOrgaoEmitente(LONG_STRING_64_ALT1);
        avulsa.setReparticaoFiscalEmitente(LONG_STRING_64_ALT2);
        avulsa.setUf(DFUnidadeFederativa.SC);
        avulsa.setValorTotalConstanteDocumentoArrecadacaoReceita(BIG_DECIMAL_999999999999_99);
        avulsa.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirMatriculaAgenteNulo() {
        final NFNotaInfoAvulsa avulsa = new NFNotaInfoAvulsa();
        avulsa.setCnpj(VALID_LENGTH_CNPJ);
        avulsa.setDataEmissaoDocumentoArrecadacao(LocalDate.of(2014, 1, 13));
        avulsa.setDataPagamentoDocumentoArrecadacao(LocalDate.of(2014, 3, 21));
        avulsa.setFone(VALID_FONE);
        avulsa.setNomeAgente(LONG_STRING_60);
        avulsa.setNumeroDocumentoArrecadacaoReceita(LONG_STRING_56);
        avulsa.setOrgaoEmitente(LONG_STRING_64_ALT1);
        avulsa.setReparticaoFiscalEmitente(LONG_STRING_64_ALT2);
    }
}

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