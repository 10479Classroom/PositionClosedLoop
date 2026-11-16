# 家庭作业：电梯闭环调参

用 **Phoenix Tuner X** 调整电梯仿真的 kG、kP、kD，使电梯稳定到达目标高度。无需真实硬件或手柄，**不要部署到实机**。

## 模型

- Kraken X60 × 1，质量 **8 kg**，减速比 **10:1**，卷筒半径 **0.02 m**。
- 行程 **0.165–1.40 m**，开启重力，输出限制 **±8 V**。
- WPILib 2026.2.1 / Phoenix 6 26.3.0；初始增益为零。
- 使用 `PositionVoltage`、Slot 0、`Elevator_Static`；kI、kS、kV、kA 保持为零。

## 启动

1. 用 WPILib VS Code 打开项目，运行 **Simulate Robot Code**，选择 Simulation GUI。
2. Tuner X 连接 **localhost:1250**，选择 **Talon FX ID 1**。
3. 打开 Glass，连接本机 NetworkTables，在 `SmartDashboard` 下操作：
   - `Elevator/TargetMeters`：目标高度（m）。
   - `Elevator/OpenLoop`：开环电压模式开关。
   - `Elevator/OpenLoopVolts`：开环电压（V）。
   - `Elevator/ResetToMidHeight`：Disabled 时设为 true，复位到 0.6 m。
   - `Elevator/HeightMeters`：实际高度。
4. 在 Simulation GUI 中选择 **Teleoperated / Enabled**。Tuner 只用于修改 Slot 0 参数并 Apply、绘制曲线，不同时发送控制命令。

也可运行 `./gradlew simulateJava`（Windows：`gradlew.bat simulateJava`）。

## 作业任务

1. **调 kG**：Disabled 时开启开环并复位；Enabled 后从 0 V 逐渐增加电压，找出电梯在行程中部悬停的电压，填入 kG。每次试验先重新复位，不要在底部判断悬停。
2. **调 kP、kD**：关闭开环，依次设置目标 `0.30 → 1.00 → 0.60 → 0.30 m`，每段保持至少 4 秒。先调 kP，再按需加 kD，每次只改一个参数，记录目标与实际位置曲线。
3. **对比实验**：在 `Robot.java` 中分别改为 **12 kg / 10:1** 和 **8 kg / 5:1**。每次先用基准增益测试，再重新调参，比较高度响应。

注意：
- Tuner 位置单位为**电机转子圈数**：`圈数 = 高度 × 减速比 ÷ (2π × 卷筒半径)`。基准模型每米约 79.577 圈，底部不是零圈。
- 改减速比应修改代码中的 `kGearRatio`，不要改 Tuner 的反馈比值（保持为 1）。
- **重启会覆盖 Tuner 参数**。完成后把增益写回 `configs.Slot0`，再重启验证。

## 验收与提交

教学目标：每次阶跃 **2 秒内进入 ±0.02 m**，并保持到本段结束；超调不超过阶跃幅值的 **10%**，无持续振荡、不碰限位。未达标也应记录并分析原因。

提交：
- 写入基准最终增益的 `Robot.java` 和 Tuner 参数截图。
- 基准调参前后、两种变更工况原增益与重调后的曲线，标明时间、目标和实际高度。
- 简短记录各工况的 kG/kP/kD、稳定误差、到位时间和超调，解释质量与减速比变化的影响。

## 验证状态

`./gradlew clean test build` 已通过（JDK 17，1 项 JUnit 测试），仿真启动检查通过。**尚未完成 Tuner X 图形界面联调和闭环动态验收。**
