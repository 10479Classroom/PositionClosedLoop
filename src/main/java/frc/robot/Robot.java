// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.Volts;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.simulation.ElevatorSim;

public class Robot extends TimedRobot {
  private static final double kMinHeightMeters = 0.165;
  private static final double kMaxHeightMeters = 1.40;
  private static final double kDrumRadiusMeters = 0.02;
  // Homework plant parameters: change ONE at a time for the comparison experiment.
  private static final double kGearRatio = 10.0; // motor rotations / drum rotation
  private static final double kCarriageMassKg = 8.0;
  private static final double kRotationsPerMeter =
      kGearRatio / (2.0 * Math.PI * kDrumRadiusMeters);

  private final TalonFX m_fx = new TalonFX(1, new CANBus("canivore"));
  private final PositionVoltage m_positionVoltage = new PositionVoltage(0).withSlot(0).withEnableFOC(false);
  private final NeutralOut m_brake = new NeutralOut();
  private final Mechanisms m_mechanism = new Mechanisms();

  private final ElevatorSim m_elevatorSim = new ElevatorSim(
      DCMotor.getKrakenX60(1),
      kGearRatio,
      kCarriageMassKg,
      kDrumRadiusMeters,
      kMinHeightMeters,
      kMaxHeightMeters,
      true,
      kMinHeightMeters);

  private double m_targetHeightMeters = kMinHeightMeters;

  public Robot() {
    if (isReal()) {
      throw new IllegalStateException("Simulation homework only: do not deploy to a real robot.");
    }
    TalonFXConfiguration configs = new TalonFXConfiguration();
    // Start un-tuned. Edit Slot 0 in Phoenix Tuner X during a run.
    // After tuning, copy your gains here to retain them across program restarts.
    configs.Slot0.kP = 0.0;
    configs.Slot0.kI = 0.0;
    configs.Slot0.kD = 0.0;
    configs.Slot0.kG = 0.0;
    configs.Slot0.GravityType = GravityTypeValue.Elevator_Static;
    configs.Voltage.withPeakForwardVoltage(Volts.of(8))
        .withPeakReverseVoltage(Volts.of(-8));

    StatusCode status = StatusCode.StatusCodeNotInitialized;
    for (int i = 0; i < 5; ++i) {
      status = m_fx.getConfigurator().apply(configs);
      if (status.isOK()) {
        break;
      }
    }
    if (!status.isOK()) {
      System.out.println("Could not apply configs, error code: " + status);
    }
    SmartDashboard.putNumber("Elevator/TargetMeters", 0.6);
    SmartDashboard.putBoolean("Elevator/ResetToMidHeight", false);
  }

  @Override
  public void robotPeriodic() {
    m_mechanism.update(m_elevatorSim.getPositionMeters(), kMinHeightMeters);
    SmartDashboard.putNumber("Elevator/AppliedTargetMeters", m_targetHeightMeters);
    SmartDashboard.putNumber("Elevator/ErrorMeters",
        m_targetHeightMeters - m_elevatorSim.getPositionMeters());
    SmartDashboard.putNumber("Elevator/VelocityMetersPerSecond", m_elevatorSim.getVelocityMetersPerSecond());
    SmartDashboard.putNumber("Elevator/MotorVolts", m_fx.getMotorVoltage().getValueAsDouble());
    SmartDashboard.putNumber("Elevator/RotorRotations", m_fx.getPosition().getValueAsDouble());
  }

  @Override
  public void teleopPeriodic() {
    m_targetHeightMeters = safeTarget(
        SmartDashboard.getNumber("Elevator/TargetMeters", m_targetHeightMeters), m_targetHeightMeters);
    m_fx.setControl(m_positionVoltage.withPosition(m_targetHeightMeters * kRotationsPerMeter));
  }

  static double safeTarget(double requested, double previous) {
    return Double.isFinite(requested)
        ? MathUtil.clamp(requested, kMinHeightMeters, kMaxHeightMeters) : previous;
  }

  @Override
  public void disabledInit() {
    m_fx.setControl(m_brake);
  }

  @Override
  public void simulationInit() {
    TalonFXSimState motorSim = m_fx.getSimState();
    motorSim.setMotorType(TalonFXSimState.MotorType.KrakenX60);
    motorSim.setSupplyVoltage(12.0);
    // Raw simulated sensor is already in absolute-height coordinates.
    // Do not also call setPosition: that would introduce a separate sensor offset.
    motorSim.setRawRotorPosition(kMinHeightMeters * kRotationsPerMeter);
  }

  @Override
  public void simulationPeriodic() {
    TalonFXSimState motorSim = m_fx.getSimState();
    // Simulation-only reset, held while disabled so gravity cannot spoil the starting point.
    if (isDisabled() && SmartDashboard.getBoolean("Elevator/ResetToMidHeight", false)) {
      m_elevatorSim.setState(0.6, 0.0);
    } else {
      SmartDashboard.putBoolean("Elevator/ResetToMidHeight", false);
      m_elevatorSim.setInputVoltage(motorSim.getMotorVoltage());
      m_elevatorSim.update(0.02);
    }

    motorSim.setRawRotorPosition(m_elevatorSim.getPositionMeters() * kRotationsPerMeter);
    motorSim.setRotorVelocity(m_elevatorSim.getVelocityMetersPerSecond() * kRotationsPerMeter);
    motorSim.setSupplyVoltage(12.0);
  }
}
