```java
package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoItemProdutoDeclaracaoImportacaoAdicaoTest {

    private static final String CODIGO_FABRICANTE = "sA2FBRFMMNgF1AKRDDXYOlc3zGvzEc69l6zQ5O5uAUe82XZ3szQfw01DW0Ki";
    private static final String DESCONTO_VALOR = "999999999999.99";
    private static final BigDecimal DESCONTO = new BigDecimal(DESCONTO_VALOR);
    private static final Integer NUMERO_VALOR = 999;
    private static final Integer SEQUENCIAL_VALOR = 999;
    private static final String NUMERO_ATO_CONCESSORIO_DRAWBACK_VALOR = "99999999999";
    private static final BigInteger NUMERO_ATO_CONCESSORIO_DRAWBACK = new BigInteger(NUMERO_ATO_CONCESSORIO_DRAWBACK_VALOR);
    private static final String XML_ESPERADO = "<NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao><nAdicao>999</nAdicao><nSeqAdic>999</nSeqAdic><cFabricante>" + CODIGO_FABRICANTE + "</cFabricante><vDescDI>" + DESCONTO_VALOR + "</vDescDI><nDraw>" + NUMERO_ATO_CONCESSORIO_DRAWBACK_VALOR + "</nDraw></NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao>";

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
        importacaoAdicao.setDesconto(DESCONTO);
        importacaoAdicao.setNumero(NUMERO_VALOR);
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoFabricanteNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setDesconto(DESCONTO);
        importacaoAdicao.setNumero(NUMERO_VALOR);
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirDescontoNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setNumero(NUMERO_VALOR);
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirNumeroNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO);
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirSequencialNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO);
        importacaoAdicao.setNumero(NUMERO_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirItemPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO);
        importacaoAdicao.setNumero(NUMERO_VALOR);
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        importacaoAdicao.toString();
    }

    @Test
    public void devePermitirNumeroPedidoCompraNulo() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setCodigoFabricante(CODIGO_FABRICANTE);
        importacaoAdicao.setDesconto(DESCONTO);
        importacaoAdicao.setNumero(NUMERO_VALOR);
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
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
        importacaoAdicao.setDesconto(DESCONTO);
        Assert.assertEquals(DESCONTO_VALOR, importacaoAdicao.getDesconto());
    }

    @Test
    public void deveObterNumeroComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setNumero(NUMERO_VALOR);
        Assert.assertEquals(NUMERO_VALOR, importacaoAdicao.getNumero());
    }

    @Test
    public void deveObterNumeroAtoConcessorioDrawbackComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setNumeroAtoConcessorioDrawback(NUMERO_ATO_CONCESSORIO_DRAWBACK);
        Assert.assertEquals(NUMERO_ATO_CONCESSORIO_DRAWBACK, importacaoAdicao.getNumeroAtoConcessorioDrawback());
    }

    @Test
    public void deveObterSequencialComoFoiSetado() {
        final NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao importacaoAdicao = new NFNotaInfoItemProdutoDeclaracaoImportacaoAdicao();
        importacaoAdicao.setSequencial(SEQUENCIAL_VALOR);
        Assert.assertEquals(SEQUENCIAL_VALOR, importacaoAdicao.getSequencial());
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        Assert.assertEquals(XML_ESPERADO, FabricaDeObjetosFake.getNFNotaInfoItemProdutoDeclaracaoImportacaoAdicao().toString());
    }
}
