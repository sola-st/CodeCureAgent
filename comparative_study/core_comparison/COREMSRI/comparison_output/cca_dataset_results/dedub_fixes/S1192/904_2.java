package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoItemProdutoDeclaracaoImportacaoAdicaoTest {

    private static final String CODIGO_FABRICANTE = "sA2FBRFMMNgF1AKRDDXYOlc3zGvzEc69l6zQ5O5uAUe82XZ3szQfw01DW0Ki";
    private static final String VALOR_DESCONTO = "999999999999.99";
    private static final BigDecimal BIGDECIMAL_DESCONTO = new BigDecimal(VALOR_DESCONTO);
    private static final String BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK_STR = "99999999999";
    private static final BigInteger BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK = new BigInteger(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK_STR);
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
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoFabricanteNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirDescontoNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirSequencialNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirItemPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirNumeroPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        importacaoAdicao.setNumero(NUMERO);
        importacaoAdicao.setSequencial(SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void deveObterCodigoFabricanteComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        Assert.assertEquals(CODIGO_FABRICANTE, importacaoAdicao.getCodigoFabricante());
    }

    @Test
    public void deveObterDescontoComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(BIGDECIMAL_DESCONTO);
        Assert.assertEquals(VALOR_DESCONTO, importacaoAdicao.getDesconto());
    }

    @Test
    public void deveObterNumeroComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setNumero(NUMERO);
        Assert.assertEquals(NUMERO, importacaoAdicao.getNumero());
    }

    @Test
    public void deveObterNumeroAtoConcessorioDrawbackComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setNumeroAtoConcessorioDrawback(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK);
        Assert.assertEquals(BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK, importacaoAdicao.getNumeroAtoConcessorioDrawback());
    }

    @Test
    public void deveObterSequencialComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setSequencial(SEQUENCIAL);
        Assert.assertEquals(SEQUENCIAL, importacaoAdicao.getSequencial());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        final String xmlEsperado = "<NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao><nAdicao>999</nAdicao><nSeqAdic>999</nSeqAdic><cFabricante>" + CODIGO_FABRICANTE + "</cFabricante><vDescDI>" + VALOR_DESCONTO + "</vDescDI><nDraw>" + BIGINTEGER_NUMERO_ATO_CONCESSORIO_DRAWBACK_STR + "</nDraw></NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao>";
        Assert.assertEquals(xmlEsperado, FabricaDeObjetosFake.getNFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().toString());
    }
}