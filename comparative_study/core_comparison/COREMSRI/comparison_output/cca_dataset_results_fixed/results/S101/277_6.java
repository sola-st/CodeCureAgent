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

  private static class HasIfNe implements Callable<String> {
    private final int i;

    HasIfNe(final int i) {
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
    final Mutant mutant = getFirstMutant(HasIfNe.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfNe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfNe(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfNe.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfNe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfNe(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfNe.class);
  }

  @Test
  public void shouldReplaceIFNE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfNe.class);
  }

  private static class HasIfNull implements Callable<String> {
    private final Object i;

    HasIfNull(final Object i) {
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
    final Mutant mutant = getFirstMutant(HasIfNull.class);
    final String expected = "was not null";
    assertMutantCallableReturns(new HasIfNull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfNull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNULL_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfNull.class);
    final String expected = "was null";
    assertMutantCallableReturns(new HasIfNull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfNull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNULL_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfNull.class);
  }

  @Test
  public void shouldReplaceIFNULL_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfNull.class);
  }

  private static class HasIfNonNull implements Callable<String> {
    private final Object i;

    HasIfNonNull(final Object i) {
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
    final Mutant mutant = getFirstMutant(HasIfNonNull.class);
    final String expected = "was null";
    assertMutantCallableReturns(new HasIfNonNull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfNonNull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNONNULL_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfNonNull.class);
    final String expected = "was not null";
    assertMutantCallableReturns(new HasIfNonNull(null), mutant, expected);
    assertMutantCallableReturns(new HasIfNonNull("foo"), mutant, expected);
  }

  @Test
  public void shouldReplaceIFNONNULL_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfNonNull.class);
  }

  @Test
  public void shouldReplaceIFNONNULL_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfNonNull.class);
  }

  private static class HasIfIcmpNe implements Callable<String> {
    private final int i;

    HasIfIcmpNe(final int i) {
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
    final Mutant mutant = getFirstMutant(HasIfIcmpNe.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfIcmpNe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpNe(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPNE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfIcmpNe.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfIcmpNe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpNe(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPNE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfIcmpNe.class);
  }

  @Test
  public void shouldReplaceIF_ICMPNE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfIcmpNe.class);
  }

  private static class HasIfIcmpEq implements Callable<String> {
    private final int i;

    HasIfIcmpEq(final int i) {
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
    final Mutant mutant = getFirstMutant(HasIfIcmpEq.class);
    final String expected = "was not zero";
    assertMutantCallableReturns(new HasIfIcmpEq(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpEq(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    final Mutant mutant = getFirstMutant(HasIfIcmpEq.class);
    final String expected = "was zero";
    assertMutantCallableReturns(new HasIfIcmpEq(1), mutant, expected);
    assertMutantCallableReturns(new HasIfIcmpEq(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    assertNoMutants(HasIfIcmpEq.class);
  }

  @Test
  public void shouldReplaceIF_ICMPEQ_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    assertNoMutants(HasIfIcmpEq.class);
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

  static class HasIfLe implements Callable<String> {
    private final int i;

    HasIfLe(final int i) {
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
    assertNoMutants(HasIfLe.class);
  }

  @Test
  public void shouldNotReplaceIFLE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIfLe.class);
  }

  @Test
  public void shouldReplaceIFLE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIfLe.class);
    final String expected = "was > zero";
    assertMutantCallableReturns(new HasIfLe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfLe(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFLE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIfLe.class);
    final String expected = "was <= zero";
    assertMutantCallableReturns(new HasIfLe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfLe(0), mutant, expected);
  }

  @Test
  public void shouldDescribeReplacementOfOrderCheckWithTrue() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIfLe.class);
    assertThat(mutant.getDetails().getDescription()).contains(
        " comparison check with true");
  }

  @Test
  public void shouldDescribeReplacementOfOrderCheckWithFalse() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIfLe.class);
    assertThat(mutant.getDetails().getDescription()).contains(
        " comparison check with false");
  }

  static class HasIfGe implements Callable<String> {
    private final int i;

    HasIfGe(final int i) {
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
    assertNoMutants(HasIfGe.class);
  }

  @Test
  public void shouldNotReplaceIFGE_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIfGe.class);
  }

  @Test
  public void shouldReplaceIFGE_ORDER_T() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, true));
    final Mutant mutant = getFirstMutant(HasIfGe.class);
    final String expected = "was < zero";
    assertMutantCallableReturns(new HasIfGe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfGe(0), mutant, expected);
  }

  @Test
  public void shouldReplaceIFGE_ORDER_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.ORDER, false));
    final Mutant mutant = getFirstMutant(HasIfGe.class);
    final String expected = "was >= zero";
    assertMutantCallableReturns(new HasIfGe(1), mutant, expected);
    assertMutantCallableReturns(new HasIfGe(0), mutant, expected);
  }

  static class HasIfGt implements Callable<String> {
    private final int i;

    HasIfGt(final int i) {
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
    assertNoMutants(HasIfGt.class);
  }

  @Test
  public void shouldNotReplaceIFGT_EQUAL_F() throws Exception {
    createTesteeWith(new RemoveConditionalMutator(Choice.EQUAL, false));
    assertNoMutants(HasIfGt.class);
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