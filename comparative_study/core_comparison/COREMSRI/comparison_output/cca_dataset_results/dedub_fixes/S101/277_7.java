package org.pitest.mutationtest.engine.gregor.mutators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.gregor.MutatorTestBase;
import org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator.Choice;

public class RemoveConditionalMutatorTest extends MutatorTestBase {

  @Test
  public void shouldProvideAMeaningfulName() {
    assertEquals("REMOVE_CONDITIONALS_EQUAL_IF_MUTATOR",
        new RemoveConditionalMutator(Choice.EQUAL, true).getName());
    assertEquals("REMOVE_CONDITIONALS_EQUAL_ELSE_MUTATOR",
        new RemoveConditionalMutator(Choice.EQUAL, false).getName());
    assertEquals("REMOVE_CONDITIONALS_ORDER_IF_MUTATOR",
        new RemoveConditionalMutator(Choice.ORDER, true).getName());
    assertEquals("REMOVE_CONDITIONALS_ORDER_ELSE_MUTATOR",
        new RemoveConditionalMutator(Choice.ORDER, false).getName());
  }

  private static int getZeroButPreventInlining() {
    return 0;
  }

  private static class HasIFEQ implements Callable<String> {
    private final int i;

    HasIFEQ(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i != 0) {
        return "was not zero";
      } else {
        return "was zero";
      }
    }
  }

  @Test
  public void shouldReplaceIFEQ_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIFEQ.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIFEQ(1), mutant, expected);
    assertMutantCallableReturns(new HasIFEQ(0), mutant, expected);
  }

  @Test
  public void shouldDescribeReplacementOfEqualityChecksWithTrue() {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIFEQ.class);
    assertThat(mutant.getDetails().getDescription()).contains(
        "equality check with true");
  }

  @Test
  public void shouldDescribeReplacementOfEqualityChecksWithFalse() {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIFEQ.class);
    assertThat(mutant.getDetails().getDescription()).contains(
        "equality check with false");
  }

  @Test
  public void shouldReplaceIFEQ_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIFEQ.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIFEQ(1), mutant, expected);
    assertMutantCallableReturns(new HasIFEQ(0), mutant, expected);
  }

  @Test
  public void shouldNotReplaceIFEQ_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIFEQ.class);
  }

  @Test
  public void shouldNotReplaceIFEQ_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIFEQ.class);
  }

  private static class HasIfne implements Callable<String> {
    private final int i;

    HasIfne(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i == 0) {
        return "was zero";
      } else {
        return "was not zero";
      }
    }
  }

  @Test
  public void shouldReplaceIFNE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfne.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfne(1), mutant, expected);
    assertMutantCallableReturns(new HasIfne(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfne.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfne(1), mutant, expected);
    assertMutantCallableReturns(new HasIfne(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfne.class);
  }

  @Test
  public void shouldReplaceIFNE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfne.class);
  }

  private static class HasIfnull implements Callable<String> {
    private final Object i;

    HasIfnull(final Object i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i != null) {
        return "was not null";
      } else {
        return "was null";
      }
    }
  }

  @Test
  public void shouldReplaceIFNULL_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfnull.class);
    final String expected = "was not null";
    assertMutantCallableReturns(new HasIfnull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfnull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNULL_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfnull.class);
    final String expected = "was null";
    assertMutantCallableReturns(new HasIfnull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfnull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNULL_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfnull.class);
  }

  @Test
  public void shouldReplaceIFNULL_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfnull.class);
  }

  private static class HasIfnonnull implements Callable<String> {
    private final Object i;

    HasIfnonnull(final Object i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i == null) {
        return "was null";
      } else {
        return "was not null";
      }
    }
  }

  @Test
  public void shouldReplaceIFNONNULL_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfnonnull.class);
    final String expected = "was null";
    assertMutantCallableReturns(new HasIfnonnull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfnonnull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNONNULL_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfnonnull.class);
    final String expected = "was not null";
    assertMutantCallableReturns(new HasIfnonnull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfnonnull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNONNULL_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfnonnull.class);
  }

  @Test
  public void shouldReplaceIFNONNULL_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfnonnull.class);
  }

  private static class HasIfIcmpne implements Callable<String> {
    private final int i;

    HasIfIcmpne(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      final int j = getZeroButPreventInlining();
      if (this.i == j) {
        return "was zero";
      } else {
        return "was not zero";
      }
    }
  }

  @Test
  public void shouldReplaceIF_ICMPNE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfIcmpne.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfIcmpne(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpne(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPNE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfIcmpne.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfIcmpne(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpne(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPNE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfIcmpne.class);
  }

  @Test
  public void shouldReplaceIF_ICMPNE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfIcmpne.class);
  }

  private static class HasIfIcmpeq implements Callable<String> {
    private final int i;

    HasIfIcmpeq(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      final int j = getZeroButPreventInlining();
      if (this.i != j) {
        return "was not zero";
      } else {
        return "was zero";
      }
    }
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfIcmpeq.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfIcmpeq(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpeq(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfIcmpeq.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfIcmpeq(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpeq(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfIcmpeq.class);
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfIcmpeq.class);
  }

  static class HasIfAcmpeq implements Callable<String> {
    private final Object i;

    HasIfAcmpeq(final Object i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i != this) {
        return "was not zero";
      } else {
        return "was zero";
      }
    }
  }

  @Test
  public void shouldReplaceIF_ACMPEQ_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfAcmpeq.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfAcmpeq(1), mutant, expected);
    assertMutantCallableReturns(new HasIfAcmpeq(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ACMPEQ_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfAcmpeq.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfAcmpeq(1), mutant, expected);
    assertMutantCallableReturns(new HasIfAcmpeq(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ACMPEQ_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfAcmpeq.class);
  }

  @Test
  public void shouldReplaceIF_ACMPEQ_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfAcmpeq.class);
  }

  static class HasIfAcmpne implements Callable<String> {
    private final Object i;

    HasIfAcmpne(final Object i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i == this) {
        return "was not zero";
      } else {
        return "was zero";
      }
    }
  }

  @Test
  public void shouldReplaceIF_ACMPNE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    final Mutant mutant = getFirstMutant(HasIfAcmpne.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfAcmpne(1), mutant, expected);
    assertMutantCallableReturns(new HasIfAcmpne(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ACMPNE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfAcmpne.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfAcmpne(1), mutant, expected);
    assertMutantCallableReturns(new HasIfAcmpne(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ACMPNE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfAcmpne.class);
  }

  @Test
  public void shouldReplaceIF_ACMPNE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfAcmpne.class);
  }

  static class HasIfle implements Callable<String> {
    private final int i;

    HasIfle(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i > 0) {
        return "was > zero";
      } else {
        return "was <= zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIFLE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIfle.class);
  }

  @Test
  public void shouldNotReplaceIFLE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIfle.class);
  }

  @Test
  public void shouldReplaceIFLE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIfle.class);
    final String expected = "was > zero";
    assertMutantCallableReturns(new HasIfle(1), mutant, expected);
    assertMutantCallableReturns(new HasIfle(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFLE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIfle.class);
    final String expected = "was <= zero";
    assertMutantCallableReturns(new HasIfle(1), mutant, expected);
    assertMutantCallableReturns(new HasIfle(0), mutant, expected);
  }

  @Test
  public void shouldDescribeReplacementOfOrderCheckWithTrue() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIfle.class);
    assertThat(mutant.getDetails().getDescription()).contains(
        " comparison check with true");
  }

  @Test
  public void shouldDescribeReplacementOfOrderCheckWithFalse() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIfle.class);
    assertThat(mutant.getDetails().getDescription()).contains(
        " comparison check with false");
  }

  static class HasIfge implements Callable<String> {
    private final int i;

    HasIfge(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i < 0) {
        return "was < zero";
      } else {
        return "was >= zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIFGE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIfge.class);
  }

  @Test
  public void shouldNotReplaceIFGE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIfge.class);
  }

  @Test
  public void shouldReplaceIFGE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIfge.class);
    final String expected = "was < zero";
    assertMutantCallableReturns(new HasIfge(1), mutant, expected);
    assertMutantCallableReturns(new HasIfge(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFGE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIfge.class);
    final String expected = "was >= zero";
    assertMutantCallableReturns(new HasIfge(1), mutant, expected);
    assertMutantCallableReturns(new HasIfge(0), mutant, expected);
  }

  static class HasIfgt implements Callable<String> {
    private final int i;

    HasIfgt(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i <= 0) {
        return "was <= zero";
      } else {
        return "was > zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIFGT_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIfgt.class);
  }

  @Test
  public void shouldNotReplaceIFGT_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIfgt.class);
  }

  @Test

  public void shouldReplaceIFGT_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIFGT.class);
    final String expected = "was > zero";
    assertMutantCallableReturns(new HasIFGT(1), mutant, expected);
    assertMutantCallableReturns(new HasIFGT(0), mutant, expected);
  }

  static class HasIFLT implements Callable<String> {
    private final int i;

    HasIFLT(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      if (this.i >= 0) {
        return "was >= zero";
      } else {
        return "was < zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIFLT_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIFLT.class);
  }

  @Test
  public void shouldNotReplaceIFLT_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIFLT.class);
  }

  @Test
  public void shouldReplaceIFLT_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIFLT.class);
    final String expected = "was >= zero";
    assertMutantCallableReturns(new HasIFLT(1), mutant, expected);
    assertMutantCallableReturns(new HasIFLT(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFLT_ORDER_T_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIFLT.class);
    final String expected = "was < zero";
    assertMutantCallableReturns(new HasIFLT(1), mutant, expected);
    assertMutantCallableReturns(new HasIFLT(0), mutant, expected);
  }

  static class HasIF_ICMPLE implements Callable<String> {
    private final int i;

    HasIF_ICMPLE(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      final int j = getZeroButPreventInlining();
      if (this.i > j) {
        return "was > zero";
      } else {
        return "was <= zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIF_ICMPLE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIF_ICMPLE.class);
  }

  @Test
  public void shouldNotReplaceIF_ICMPLE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIF_ICMPLE.class);
  }

  @Test
  public void shouldReplaceIF_ICMPLE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIF_ICMPLE.class);
    final String expected = "was > zero";
    assertMutantCallableReturns(new HasIF_ICMPLE(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPLE(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPLE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIF_ICMPLE.class);
    final String expected = "was <= zero";
    assertMutantCallableReturns(new HasIF_ICMPLE(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPLE(0), mutant, expected);
  }

  static class HasIF_ICMPGE implements Callable<String> {
    private final int i;

    HasIF_ICMPGE(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      final int j = getZeroButPreventInlining();
      if (this.i < j) {
        return "was < zero";
      } else {
        return "was >= zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIF_ICMPGE_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIF_ICMPGE.class);
  }

  @Test
  public void shouldNotReplaceIF_ICMPGE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIF_ICMPGE.class);
  }

  @Test
  public void shouldReplaceIF_ICMPGE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIF_ICMPGE.class);
    final String expected = "was < zero";
    assertMutantCallableReturns(new HasIF_ICMPGE(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPGE(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPGE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIF_ICMPGE.class);
    final String expected = "was >= zero";
    assertMutantCallableReturns(new HasIF_ICMPGE(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPGE(0), mutant, expected);
  }

  static class HasIF_ICMPGT implements Callable<String> {
    private final int i;

    HasIF_ICMPGT(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      final int j = getZeroButPreventInlining();
      if (this.i <= j) {
        return "was <= zero";
      } else {
        return "was > zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIF_ICMPGT_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIF_ICMPGT.class);
  }

  @Test
  public void shouldNotReplaceIF_ICMPGT_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIF_ICMPGT.class);
  }

  @Test
  public void shouldReplaceIF_ICMPGT_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIF_ICMPGT.class);
    final String expected = "was <= zero";
    assertMutantCallableReturns(new HasIF_ICMPGT(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPGT(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPGT_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIF_ICMPGT.class);
    final String expected = "was > zero";
    assertMutantCallableReturns(new HasIF_ICMPGT(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPGT(0), mutant, expected);
  }

  static class HasIF_ICMPLT implements Callable<String> {
    private final int i;

    HasIF_ICMPLT(final int i) {
      this.i = i;
    }

    @Override
    public String call() {
      final int j = getZeroButPreventInlining();
      if (this.i >= j) {
        return "was >= zero";
      } else {
        return "was < zero";
      }
    }
  }

  @Test
  public void shouldNotReplaceIF_ICMPLT_EQUAL_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, true));
    assertNoMutants(HasIF_ICMPLT.class);
  }

  @Test
  public void shouldNotReplaceIF_ICMPLT_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIF_ICMPLT.class);
  }

  @Test
  public void shouldReplaceIF_ICMPLT_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIF_ICMPLT.class);
    final String expected = "was >= zero";
    assertMutantCallableReturns(new HasIF_ICMPLT(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPLT(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPLT_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIF_ICMPLT.class);
    final String expected = "was < zero";
    assertMutantCallableReturns(new HasIF_ICMPLT(1), mutant, expected);
    assertMutantCallableReturns(new HasIF_ICMPLT(0), mutant, expected);
  }
}