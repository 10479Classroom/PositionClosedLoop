package frc.robot;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ElevatorHomeworkTest {
  @Test
  void targetIsClampedAndNonFiniteInputsKeepLastTarget() {
    assertEquals(0.165, Robot.safeTarget(-1, 0.6));
    assertEquals(1.4, Robot.safeTarget(2, 0.6));
    assertEquals(0.6, Robot.safeTarget(Double.NaN, 0.6));
    assertEquals(0.6, Robot.safeTarget(Double.POSITIVE_INFINITY, 0.6));
    assertEquals(0.9, Robot.safeTarget(0.9, 0.6));
  }
}
