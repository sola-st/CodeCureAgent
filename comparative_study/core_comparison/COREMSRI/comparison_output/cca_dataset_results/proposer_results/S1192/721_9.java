package com.fincatto.documentofiscal.nfe400.classes.nota;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.FabricaDeObjetosFake;

public class NFNotaInfoItemProdutoCombustivelTest {

    private static final String CODIF_VALID = "Cirh89sPDDbnFAzZMPpmG";
    private static final String CODIF_INVALID = "Cirh89sPDDbnFAzZMPpmG1";
    private static final String CODIGO_PRODUTO_ANP_8 = "99999999";
    private static final String CODIGO_PRODUTO_ANP_10 = "9999999999";
    private static final String CODIGO_PRODUTO_ANP_9 = "999999999";
    private static final String DESCRICAO_ANP = "Descricao";
    private static final String UF_AC = "AC";
    private static final String UF_RS = "RS";
    private static final String XML_ESPERADO = "<NFNotaInfoItemProdutoCombustivel><cProdANP>999999999</cProdANP><descANP>descricao</descANP><CODIF>Cirh89sPDDbnFAzZMPpmG</CODIF><qTemp>99999999999.9999</qTemp><UFCons>RS</UFCons><CIDE><qBCProd>99999999999.9999</qBCProd><vAliqProd>9999999999.9999</vAliqProd><vCIDE>999999999999.99</vCIDE></CIDE></NFNotaInfoItemProdutoCombustivel>";

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoAutorizacaoCODIFComTamanhoInvalido() {
        try {
            new NFNotaInfoItemProdutoCombustivel().setCodigoAutorizacaoCODIF("");
        } catch (final IllegalStateException e) {
            new NFNotaInfoItemProdutoCombustivel().setCodigoAutorizacaoCODIF(CODIF_INVALID);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoProdutoANPComTamanhoInvalido() {
        try {
            new NFNotaInfoItemProdutoCombustivel().setCodigoProdutoANP(CODIGO_PRODUTO_ANP_8);
        } catch (final IllegalStateException e) {
            new NFNotaInfoItemProdutoCombustivel().setCodigoProdutoANP(CODIGO_PRODUTO_ANP_10);
        }
    }

    @Test(expected = NumberFormatException.class)
    public void naoDevePermitirQuantidadeComTamanhoInvalido() {
        new NFNotaInfoItemProdutoCombustivel().setQuantidade(new BigDecimal("1000000000000"));
    }

    @Test
    public void devePermitirCideNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCodigoAutorizacaoCODIF(CODIF_VALID);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_9);
        combustivel.setDescricaoProdutoANP(DESCRICAO_ANP);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.valueOf(UF_AC));
        combustivel.toString();
    }

    @Test
    public void devePermitirCodigoAutorizacaoCODIFNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_9);
        combustivel.setDescricaoProdutoANP(DESCRICAO_ANP);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.valueOf(UF_AC));
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirCodigoProdutoANPNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIF_VALID);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.valueOf(UF_AC));
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirDescricaoProdutoANPNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIF_VALID);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_9);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.valueOf(UF_AC));
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void descricaoANPDeveTer9Digitos() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIF_VALID);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.setUf(DFUnidadeFederativa.valueOf(UF_AC));
        combustivel.toString();
    }

    @Test
    public void devePermitirQuantidadeNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIF_VALID);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_9);
        combustivel.setUf(DFUnidadeFederativa.valueOf(UF_AC));
        combustivel.setDescricaoProdutoANP(DESCRICAO_ANP);
        combustivel.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void naoDevePermitirUFNulo() {
        final NFNotaInfoItemProdutoCombustivel combustivel = new NFNotaInfoItemProdutoCombustivel();
        combustivel.setCide(FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivelCIDE());
        combustivel.setCodigoAutorizacaoCODIF(CODIF_VALID);
        combustivel.setCodigoProdutoANP(CODIGO_PRODUTO_ANP_9);
        combustivel.setQuantidade(new BigDecimal("99999999999.9999"));
        combustivel.toString();
    }

    @Test
    public void deveGerarXMLDeAcordoComOPadraoEstabelecido() {
        Assert.assertEquals(XML_ESPERADO, FabricaDeObjetosFake.getNFNotaInfoItemProdutoCombustivel().toString());
    }
}