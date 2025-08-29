package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoItemProdutoCombustivelTest {

    private static final String CODIGO_AUTORIZACAO_CODIF_VALIDA = "Cirh89sPDDbnFAzZMPpmG";
    private static final String CODIGO_PRODUTO_ANP_VALIDO = "999999999";
    private static final String DESCRICAO_PRODUTO_ANP_VALIDA = "Descricao";
    private static final String UF_RS = "RS";

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoAutorizacaoCODIFComTamanhoInvalido() {
        try {
            new NFNotaInfoItemProdutoCombustivel().setCodigoAutorizacaoCODIF("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoItemProdutoCombustivel().setCodigoAutorizacaoCODIF("Cirh89sPDDbnFAzZMPpmG1");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoProdutoANPComTamanhoInvalido() {
        try {
            new NFNotaInfoItemProdutoCombustivel().setCodigoProdutoANP("99999999");
        } catch (final IllegalStateException e) {
            new NFNotaInfoItemProdutoCombustivel().setCodigoProdutoANP("9999999999");
        }
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirQuantidadeComTamanhoInvalido() {
        new NFNotaInfoItemProdutoCombustivel().setQuantidade(new BigDecimal("1000000000000"));
    }

    @Test
    public void devePermitirCideNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCodigoAutorizacaoCODIF(CODIGO_AUTORIZACAO_CODIF_VALIDA);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_VALIDO);
        combustivel.setDescricaoProdutoANP(DESCRICAO_PRODUTO_ANP_VALIDA);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.AC);
        combustivel.toString();
    }

    @Test
    public void devePermitirCodigoAutorizacaoCODIFNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_VALIDO);
        combustivel.setDescricaoProdutoANP(DESCRICAO_PRODUTO_ANP_VALIDA);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.AC);
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoProdutoANPNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIGO_AUTORIZACAO_CODIF_VALIDA);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.AC);
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirDescricaoProdutoANPNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIGO_AUTORIZACAO_CODIF_VALIDA);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_VALIDO);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.AC);
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void descricaoANPDeveTer9Digitos() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIGO_AUTORIZACAO_CODIF_VALIDA);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.AC);
        combustivel.toString();
    }

    @Test
    public void devePermitirQuantidadeNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIGO_AUTORIZACAO_CODIF_VALIDA);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_VALIDO);
        combustivel.setUf(DFUnidadeFederativa.AC);
        combustivel.setDescricaoProdutoANP(DESCRICAO_PRODUTO_ANP_VALIDA);
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirUFNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIGO_AUTORIZACAO_CODIF_VALIDA);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_VALIDO);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.toString();
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        final String xmlEsperado = "<NFNotaInfoItemProdutoCombustivel><cProdANP>999999999</cProdANP><descANP>descricao</descANP><CODIF>Cirh89sPDDbnFAzZMPpmG</CODIF><qTemp>99999999999.9999</qTemp><UFCons>RS</UFCons><CIDE><qBCProd>99999999999.9999</qBCProd><vAliqProd>9999999999.9999</vAliqProd><vCIDE>999999999999.99</vCIDE></CIDE></NFNotaInfoItemProdutoCombustivel>";
        Assert.assertEquals(xmlEsperado, FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivel().toString());
    }
}