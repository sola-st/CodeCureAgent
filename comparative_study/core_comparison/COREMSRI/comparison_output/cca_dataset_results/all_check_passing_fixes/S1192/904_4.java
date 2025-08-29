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
    private static final Integer VALOR_NUMERO = 999;
    private static final Integer VALOR_SEQUENCIAL = 999;
    private static final BigInteger VALOR_NUMERO_ATO_CONCESSORIO = new BigInteger("99999999999");
    private static final String XML_ESPERADO = "<NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao><nAdicao>999</nAdicao><nSeqAdic>999</nSeqAdic><cFabricante>sA2FBRFMMNgF1AKRDDXYOlc3zGvzEc69l6zQ5O5uAUe82XZ3szQfw01DW0Ki</cFabricante><vDescDI>999999999999.99</vDescDI><nDraw>99999999999</nDraw></NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao>";

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
        importacaoAdicao.setNumero(VALOR_NUMERO);
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoFabricanteNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_NUMERO);
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirDescontoNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setNumero(VALOR_NUMERO);
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirSequencialNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_NUMERO);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirItemPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_NUMERO);
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirNumeroPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        importacaoAdicao.setNumero(VALOR_NUMERO);
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
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
        importacaoAdicao.setDesconto(DESCONTO_BIGDECIMAL);
        Assert.assertEquals(VALOR_DESCONTO, importacaoAdicao.getDesconto());
    }

    @Test
    public void deveObterNumeroComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setNumero(VALOR_NUMERO);
        Assert.assertEquals(VALOR_NUMERO, importacaoAdicao.getNumero());
    }

    @Test
    public void deveObterNumeroAtoConcessorioDrawbackComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setNumeroAtoConcessorioDrawback(VALOR_NUMERO_ATO_CONCESSORIO);
        Assert.assertEquals(VALOR_NUMERO_ATO_CONCESSORIO, importacaoAdicao.getNumeroAtoConcessorioDrawback());
    }

    @Test
    public void deveObterSequencialComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setSequencial(VALOR_SEQUENCIAL);
        Assert.assertEquals(VALOR_SEQUENCIAL, importacaoAdicao.getSequencial());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        Assert.assertEquals(XML_ESPERADO, FabricaDeObjetosFake.getNFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().toString());
    }
}