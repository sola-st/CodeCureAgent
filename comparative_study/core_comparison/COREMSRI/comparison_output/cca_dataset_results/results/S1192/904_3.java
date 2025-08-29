package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoItemProdutoDeclaracaoImportacaoAdicaoTest {

    private static final String CODIGO_FABRICANTE = "sA2FBRFMMNgF1AKRDDXYOlc3zGvzEc69l6zQ5O5uAUe82XZ3szQfw01DW0Ki";
    private static final String DESCONTO_VALOR = "999999999999.99";
    private static final BigDecimal DESCONTO_BIGDECIMAL = new BigDecimal(DESCONTO_VALOR);
    private static final String NUMERO_ATO_CONCESSORIO_DRAWBACK_VALOR = "99999999999";
    private static final BigInteger NUMERO_ATO_CONCESSORIO_DRAWBACK = new BigInteger(NUMERO_ATO_CONCESSORIO_DRAWBACK_VALOR);
    private static final Integer NUMERO = 999;
    private static final Integer SEQUENCIAL = 999;

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
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoFabricanteNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirDescontoNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirSequencialNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirItemPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirNumeroPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
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
        Assert.assertEquals(DESCONTO_VALOR, importacaoAdicao.getDesconto());
    }

    @Test
    public void deveObterNumeroComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        final Integer numero = NUMERO;
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
        final Integer sequencial = SEQUENCIAL;
        importacaoAdicao.setSequencial(sequencial);
        Assert.assertEquals(sequencial, importacaoAdicao.getSequencial());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        final String xmlEsperado = "<NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao><nAdicao>999</nAdicao><nSeqAdic>999</nSeqAdic><cFabricante>" + CODIGO_FABRICANTE + "</cFabricante><vDescDI>" + DESCONTO_VALOR + "</vDescDI><nDraw>" + NUMERO_ATO_CONCESSORIO_DRAWBACK_VALOR + "</nDraw></NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao>";
        Assert.assertEquals(xmlEsperado, FabricaDeObjetosFake.getNFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().toString());
    }
}