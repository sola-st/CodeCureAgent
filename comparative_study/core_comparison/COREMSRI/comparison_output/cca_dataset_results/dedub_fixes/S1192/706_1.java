package com.fincatto.documentofiscal.nfe310.classes.nota;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.nfe310.FabricaDeObjetosFake;
import com.fincatto.documentofiscal.nfe310.classes.nota.NFNotaInfoRetencaoICMSTransporte;

public class NFNotaInfoRetencaoICMSTransporteTest {

    private static final String VALID_ALIQUOTA_RETENCAO = "99.99";
    private static final String VALID_VALOR_ICMS_RETIDO = "999999999999";
    private static final String VALID_BC_RETENCAO_ICMS = "999999999999";
    private static final String VALID_VALOR_SERVICO = "999999999999";
    private static final String INVALID_ALIQUOTA_RETENCAO = "100000";
    private static final String INVALID_VALOR_ICMS_RETIDO = "1000000000000000";
    private static final String INVALID_BC_RETENCAO_ICMS = "1000000000000000";
    private static final String INVALID_VALOR_SERVICO = "1000000000000000";
    private static final String INVALID_CODIGO_MUNICIPIO = "10000000";
    private static final String VALID_CODIGO_MUNICIPIO = "9999999";
    private static final String VALID_VALOR_BD = "999999999999.99";

    @Test
    public void devePermitirAliquotaRetencaoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setAliquotaRetencao(new BigDecimal(VALID_ALIQUOTA_RETENCAO));
    }

    @Test
    public void devePermitirValorICMSRetidoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorICMSRetido(new BigDecimal(VALID_VALOR_ICMS_RETIDO));
    }

    @Test
    public void devePermitirValorBXRetencaoICMSTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setBcRetencaoICMS(new BigDecimal(VALID_BC_RETENCAO_ICMS));
    }

    @Test
    public void devePermitirValorICMSRetidoValorServicoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorServico(new BigDecimal(VALID_VALOR_SERVICO));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirAliquotaRetencaoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setAliquotaRetencao(new BigDecimal(INVALID_ALIQUOTA_RETENCAO));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorICMSRetidoTamanhoInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorICMSRetido(new BigDecimal(INVALID_VALOR_ICMS_RETIDO));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirBCRetencaoICMSTamanhoInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setBcRetencaoICMS(new BigDecimal(INVALID_BC_RETENCAO_ICMS));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorServicoTamanhoInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorServico(new BigDecimal(INVALID_VALOR_SERVICO));
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoMunicipioOcorrenciaFatoGeradorICMSTransporteInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(INVALID_CODIGO_MUNICIPIO);
    }

    @Test
    public void devePermitirCodigoMunicipioOcorrenciaFatoGeradorICMSTransporteValido() {
        new NFNotaInfoRetencaoICMSTransporte().setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(VALID_CODIGO_MUNICIPIO);
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirCFOPValorInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setCfop(10000);
    }

    @Test
    public void devePermitirCFOPValorValido() {
        new NFNotaInfoRetencaoICMSTransporte().setCfop(5351);
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirValorServicoNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(VALID_ALIQUOTA_RETENCAO));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(VALID_CODIGO_MUNICIPIO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirValorICMSRetidoNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(VALID_ALIQUOTA_RETENCAO));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(VALID_CODIGO_MUNICIPIO);
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoMunicipioOcorrenciaFatoGeradorICMSTransporteNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(VALID_ALIQUOTA_RETENCAO));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCfopNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(VALID_ALIQUOTA_RETENCAO));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(VALID_CODIGO_MUNICIPIO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirBcRetencaOICMSNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(VALID_ALIQUOTA_RETENCAO));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(VALID_CODIGO_MUNICIPIO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirAliquotaRetencaoNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(VALID_CODIGO_MUNICIPIO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALID_VALOR_BD));
        retencaoICMSTransporte.toString();
    }

    @Test
    public void deveObterAliquotaRetencaoComoFoiSetado() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(BigDecimal.ONE);
        Assert.assertEquals("1.00", retencaoICMSTransporte.getAliquotaRetencao());
    }

    @Test
    public void deveObterBcRetencaoICMSComoFoiSetado() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setBcRetencaoICMS(BigDecimal.ONE);
        Assert.assertEquals("1.00", retencaoICMSTransporte.getBcRetencaoICMS());
    }

    @Test
    public void deveObterCfopComoFoiSetado() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setCfop(193);
        Assert.assertEquals(193, retencaoICMSTransporte.getCfop(), 0);
    }

    @Test
    public void deveObterCodigoMunicioOcorrenciaFatoGeradorICMSTransporteComoFoiSetado() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        final String codigoMunicioOcorrenciaFatoGeradorICMSTransporte = "9876541";
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(codigoMunicioOcorrenciaFatoGeradorICMSTransporte);
        Assert.assertEquals(codigoMunicioOcorrenciaFatoGeradorICMSTransporte, retencaoICMSTransporte.getCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte());
    }

    @Test
    public void deveObterValorICMSRetidoComoFoiSetado() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setValorICMSRetido(BigDecimal.TEN);
        Assert.assertEquals("10.00", retencaoICMSTransporte.getValorICMSRetido());
    }

    @Test
    public void deveObterValorServicoComoFoiSetado() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        final String valorServico = "100.00";
        retencaoICMSTransporte.setValorServico(new BigDecimal(valorServico));
        Assert.assertEquals(valorServico, retencaoICMSTransporte.getValorServico());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        final String xmlEsperado = "<NFNotaInfoRetencaoICMSTransporte><vServ>999999999999.99</vServ><vBCRet>999999999999.99</vBCRet><pICMSRet>99.99</pICMSRet><vICMSRet>999999999999.99</vICMSRet><CFOP>5351</CFOP><cMunFG>9999999</cMunFG></NFNotaInfoRetencaoICMSTransporte>";
        Assert.assertEquals(xmlEsperado, FabricaDeObjetosFake.getNFNotaInfoRetencaoICMSTransporte().toString());
    }
}