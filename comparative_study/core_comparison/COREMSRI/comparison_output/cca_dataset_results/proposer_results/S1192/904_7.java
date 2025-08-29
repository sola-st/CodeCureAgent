package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoItemProdutoDeclaracaoImportacaoAdicaoTest {

    private static final String CODIGO_FABRICANTE = "sA2FBRFMMNgF1AKRDDXYOlc3zGvzEc69l6zQ5O5uAUe82XZ3szQfw01DW0Ki";
    private static final String VALOR_DESCONTO = "999999999999.99";
    private static final BigDecimal DESCONTO_BIGDECIMAL = new BigDecimal(VALOR_DESCONTO);
    private static final BigInteger NUMERO_ATO_CONCESSORIO_DRAWBACK = new BigInteger("99999999999");
    private static final Integer VALOR_999 = 999;

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoFabricanteComTamanhoInvalido() {
        try {
            new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().setCodigoFabricante("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().setCodigoFabricante("sA2FBRFMMNgF1AKRDDXYOlc3zGvzEc69l6zQ5O5uAUe82XZ3szQfw01DW0Ki1");
        }
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirNumeroAtoConcessorioDrawbackComTamanhoInvalido() {
        new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().setNumeroAtoConcessorioDrawback(new BigInteger("100000000000"));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirDescontoComTamanhoInvalido() {
        new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().setDesconto(new BigDecimal("10000000000000"));
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirNumeroComTamanhoInvalido() {
        new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().setNumero(1000);
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirSequencialComTamanhoInvalido() {
        new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().setSequencial(1000);
    }

    @Test
    public void devePermitirNumeroAtoConcessorioDrawbackNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_999);
        importacaoAdicao.setSequencial(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoFabricanteNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_999);
        importacaoAdicao.setSequencial(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirDescontoNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setNumero(VALOR_999);
        importacaoAdicao.setSequencial(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setSequencial(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirSequencialNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirItemPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_999);
        importacaoAdicao.setSequencial(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirNumeroPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_999);
        importacaoAdicao.setSequencial(VALOR_999);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void deveObterCodigoFabricanteComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        final String codigoFabricante = CODIGO_FABRICANTE;
        importacaoAdicao.setCodigoFabricante(codigoFabricante);
        Assert.assertEquals(codigoFabricante, importacaoAdicao.getCodigoFabricante());
    }

    @Test
    public void deveObterDescontoComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        Assert.assertEquals(VALOR_DESCONTO, importacaoAdicao.getDesconto());
    }

    @Test
    public void deveObterNumeroComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        final Integer numero = VALOR_999;
        importacaoAdicao.setNumero(numero);
        Assert.assertEquals(numero, importacaoAdicao.getNumero());
    }

    @Test
    public void deveObterNumeroAtoConcessorioDrawbackComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        final BigInteger numeroAtoConcessorioDrawback = NUMERO_ATO_CONCESSORIO_DRAWBACK;
        importacaoAdicao.setNumeroAtoConcessorioDrawback(numeroAtoConcessorioDrawback);
        Assert.assertEquals(numeroAtoConcessorioDrawback, importacaoAdicao.getNumeroAtoConcessorioDrawback());
    }

    @Test
    public void deveObterSequencialComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        final Integer sequencial = VALOR_999;
        importacaoAdicao.setSequencial(sequencial);
        Assert.assertEquals(sequencial, importacaoAdicao.getSequencial());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        final String xmlEsperado = "<NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao><nAdicao>999</nAdicao><nSeqAdic>999</nSeqAdic><cFabricante>" + CODIGO_FABRICANTE + "</cFabricante><vDescDI>" + VALOR_DESCONTO + "</vDescDI><nDraw>" + NUMERO_ATO_CONCESSORIO_DRAWBACK + "</nDraw></NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao>";
        Assert.assertEquals(xmlEsperado, FabricaDeObjetosFake.getNFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().toString());
    }
}