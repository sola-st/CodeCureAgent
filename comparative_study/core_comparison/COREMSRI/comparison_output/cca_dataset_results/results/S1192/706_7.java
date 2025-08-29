package com.fincatto.documentofiscal.nfe310.classes.nota;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.nfe310.FabricaDeObjetosFake;
import com.fincatto.documentofiscal.nfe310.classes.nota.NFNotaInfoRetencaoICMSTransporte;

public class NFNotaInfoRetencaoICMSTransporteTest {

    private static final String ALIQUOTA_RETENCAO_99_99 = "99.99";
    private static final String VALOR_999999999999 = "999999999999";
    private static final String VALOR_999999999999_99 = "999999999999.99";
    private static final String VALOR_100000 = "100000";
    private static final String VALOR_1000000000000000 = "1000000000000000";
    private static final String CODIGO_MUNICIPIO_INVALIDO = "10000000";
    private static final String CODIGO_MUNICIPIO_VALIDO = "9999999";
    private static final String VALOR_SERVICO_100 = "100.00";
    private static final String XML_ESPERADO = "<NFNotaInfoRetencaoICMSTransporte><vServ>999999999999.99</vServ><vBCRet>999999999999.99</vBCRet><pICMSRet>99.99</pICMSRet><vICMSRet>999999999999.99</vICMSRet><CFOP>5351</CFOP><cMunFG>9999999</cMunFG></NFNotaInfoRetencaoICMSTransporte>";

    @Test
    public void devePermitirAliquotaRetencaoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setAliquotaRetencao(new BigDecimal(ALIQUOTA_RETENCAO_99_99));
    }

    @Test
    public void devePermitirValorICMSRetidoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorICMSRetido(new BigDecimal(VALOR_999999999999));
    }

    @Test
    public void devePermitirValorBXRetencaoICMSTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setBcRetencaoICMS(new BigDecimal(VALOR_999999999999));
    }

    @Test
    public void devePermitirValorICMSRetidoValorServicoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorServico(new BigDecimal(VALOR_999999999999));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirAliquotaRetencaoTamanhoValido() {
        new NFNotaInfoRetencaoICMSTransporte().setAliquotaRetencao(new BigDecimal(VALOR_100000));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorICMSRetidoTamanhoInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorICMSRetido(new BigDecimal(VALOR_1000000000000000));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirBCRetencaoICMSTamanhoInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setBcRetencaoICMS(new BigDecimal(VALOR_1000000000000000));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirValorServicoTamanhoInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setValorServico(new BigDecimal(VALOR_1000000000000000));
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoMunicipioOcorrenciaFatoGeradorICMSTransporteInvalido() {
        new NFNotaInfoRetencaoICMSTransporte().setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_INVALIDO);
    }

    @Test
    public void devePermitirCodigoMunicipioOcorrenciaFatoGeradorICMSTransporteValido() {
        new NFNotaInfoRetencaoICMSTransporte().setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_VALIDO);
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
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(ALIQUOTA_RETENCAO_99_99));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_VALIDO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirValorICMSRetidoNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(ALIQUOTA_RETENCAO_99_99));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_VALIDO);
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoMunicipioOcorrenciaFatoGeradorICMSTransporteNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(ALIQUOTA_RETENCAO_99_99));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCfopNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(ALIQUOTA_RETENCAO_99_99));
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_VALIDO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirBcRetencaOICMSNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setAliquotaRetencao(new BigDecimal(ALIQUOTA_RETENCAO_99_99));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_VALIDO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirAliquotaRetencaoNulo() {
        final NFNotaInfoRetencaoICMSTransporte retencaoICMSTransporte = new NFNotaInfoRetencaoICMSTransporte();
        retencaoICMSTransporte.setBcRetencaoICMS(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setCfop(5351);
        retencaoICMSTransporte.setCodigoMunicipioOcorrenciaFatoGeradorICMSTransporte(CODIGO_MUNICIPIO_VALIDO);
        retencaoICMSTransporte.setValorICMSRetido(new BigDecimal(VALOR_999999999999_99));
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALOR_999999999999_99));
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
        retencaoICMSTransporte.setValorServico(new BigDecimal(VALOR_SERVICO_100));
        Assert.assertEquals(VALOR_SERVICO_100, retencaoICMSTransporte.getValorServico());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        Assert.assertEquals(XML_ESPERADO, FabricaDeObjetosFake.getNFNotaInfoRetencaoICMSTransporte().toString());
    }
}