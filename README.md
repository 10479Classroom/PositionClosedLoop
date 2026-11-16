# 家庭作业：用 Phoenix Tuner X 调好一个电梯

**目标：不用真实电机，也不用手柄，在带质量、减速器和重力的电梯仿真中，用 CTRE Phoenix Tuner X 完成位置闭环调参。**

建议用时 60–90 分钟。重点是实验和读曲线，不是抄一组 PID。此项目仅用于桌面仿真，代码禁止实机运行；不要部署到机器人。

## 1. 给定模型

沿用仓库 WPILib **2026.2.1**、Phoenix 6 **26.3.0**，不需要 CANivore 或真实 Talon FX。

| 参数 | 基准值 |
|---|---:|
| 电机 | 1 个 Kraken X60，非 FOC 电压控制 |
| 运动部分总质量 | 8 kg |
| 减速比 | 10:1（电机转 10 圈，卷筒转 1 圈） |
| 卷筒半径 | 0.020 m |
| 高度范围 | 0.165–1.400 m |
| 电源 / 输出限制 | 12 V / ±8 V |
| 重力 | 开启 |
| 控制 | `PositionVoltage`，Slot 0，`Elevator_Static` |
| 初始增益 | kP、kI、kD、kG 均为 0 |

物理参数在 `src/main/java/frc/robot/Robot.java` 顶部。它是理想 `ElevatorSim`，没有静摩擦、皮带弹性、间隙或电池压降。**本题不调 kS，保持 kI、kS、kV、kA 为 0。** 停电时滑落是重力的结果，不是图形显示错误。

### 单位必须先弄清

保持 Tuner 中反馈比值为初始值（1）；本项目控制器的位置单位是**电机转子圈数**，不是米，也不是卷筒圈数：

`电机圈数 = 高度(m) × 减速比 ÷ (2π × 卷筒半径)`

基准模型每米约 **79.577 圈**。0.3 / 0.6 / 1.0 m 分别约 23.873 / 47.746 / 79.577 圈。本项目使用绝对高度，底部 0.165 m 不是编码器零圈。

因此 kP 单位为 V/圈，kD 为 V/(圈/秒)，kG 为 V。不要把以米为单位的其他项目增益直接抄过来。

## 2. 启动与连接

推荐在同一台安装了 WPILib 2026 和 Phoenix Tuner X 的 Windows 电脑上完成。

1. 用 WPILib VS Code 打开此文件夹，执行 **WPILib: Simulate Robot Code**，选择桌面 Simulation GUI。也可运行 `./gradlew simulateJava`（Windows：`gradlew.bat simulateJava`）。
2. 打开 Tuner X，连接 **localhost**，默认诊断端口 **1250**。应该看到 **Talon FX ID 1**。仿真不要求物理 CANivore；程序中的总线名字不代表要接硬件。
3. 打开 **Glass**，连接本机 NetworkTables（localhost）。在 NetworkTables 的 `SmartDashboard` 下操作以下字段；`Elevator` 是 Mechanism2d 图形。
4. 在 Simulation GUI 的 Driver Station 面板中选择 **Teleoperated / Enabled** 才会执行控制。无需手柄。实验结束切回 Disabled。
5. 在 Tuner 的 Configs 中修改 **Slot 0** 并执行应用操作。用 Plot/Signal & Control 的绘图功能观察 Position、Velocity、Motor Voltage、Closed Loop Reference/Error（具体标签随 Tuner 版本而异）。**不要同时用 Tuner 的控制按钮发命令**，本作业由机器人程序发送目标，Tuner 只负责改增益和画图。

| Glass / SmartDashboard 字段 | 用法 |
|---|---|
| `Elevator/TargetMeters` | 可写：目标高度，单位 m，自动限制在行程内 |
| `Elevator/OpenLoop` | 可写：false 为位置闭环；true 为开环电压实验 |
| `Elevator/OpenLoopVolts` | 可写：开环电压，自动限制为 ±8 V |
| `Elevator/ResetToMidHeight` | 可写：仅 Disabled 时设 true，把仿真保持在 0.6 m；Enabled 后自动解除 |
| `Elevator/HeightMeters` | 当前仿真高度 |
| `Elevator/AppliedTargetMeters` | 限幅后的目标 |
| `Elevator/ErrorMeters` | 目标减当前高度，开环实验中不作为控制误差解释 |
| `Elevator/VelocityMetersPerSecond` | 当前速度 |
| `Elevator/MotorVolts` / `Elevator/RotorRotations` | 电机输出 / 编码器位置 |

**配置保存规则：** 程序只在启动时写入配置，不会在 periodic 中覆盖 Tuner 的调参。因此同一次运行内修改会持续有效。重新启动程序会重新写入代码中的增益，恢复作业初值。完成后导出/截图 Tuner 配置，并把最终增益写回构造函数的四行 `configs.Slot0`，重启验证。不要把“点击 Apply”当作已经完成代码交付。

## 3. 必做实验

### A. 测出 kG：先能悬停，再谈位置控制

1. Disabled，设 `OpenLoop=true`、`OpenLoopVolts=0`、`ResetToMidHeight=true`。
2. Enabled，观察从 0.6 m 下落。不要在底部判断“悬停”，底部机械限位会托住电梯。
3. 每次 Disabled 后重新复位到 0.6 m，逐渐增大正电压，寻找在行程中部基本不动的电压；先用 0.05 V 左右步进，再缩小步进。若明显上升/下落，及时 Disabled 并重试。
4. 在 Tuner 的 Slot 0 中确认 Gravity Type 为 **Elevator Static**，把测到的悬停电压填入 kG，保持其他增益为 0，Apply。
5. 设 `OpenLoop=false`，目标 0.6 m，重复复位/使能，验证补偿效果。零 kP 时改变位置目标不会主动回到目标，说明 kG 不是位置控制。

记录：悬停电压、测量时实际高度、2 秒内高度变化。说明为什么不能在底部完成这项测量。

### B. 调 kP、kD：让电梯到位且不振荡

1. 保留 kG，关闭开环；kI 始终为 0。由较小的 kP 开始，例如 **0.05 V/圈**，这只是试调起点，不是答案。
2. 按 `0.30 → 1.00 → 0.60 → 0.30 m` 切换目标，每段保持至少 4 秒。
3. 每次只改一个参数，逐步提高 kP，记录响应速度、超调和振荡。不必为了制造振荡把增益无限增大。
4. 必要时小幅增加 kD，比较同一组阶跃曲线；高 kD 也可能造成抖动。若电压频繁打到 ±8 V，先减小增益，而不是提高电压限制。
5. 保存至少三组曲线：初始/明显偏弱、调整中、最终。曲线必须标明目标、实际位置、时间和单位。

**基础验收目标（教学标准，不是预先验证的调参答案）：** 对上述每个阶跃，在 2 秒内进入目标 ±0.02 m 并在余下 2 秒内保持；超调不超过阶跃幅值的 10%；不碰上下硬限位，无持续振荡。超调按运动方向计算，下行也要检查。若未达标，保留最好结果并解释限制，不得只交成功截图。

### C. 参数变化：质量与减速比各改变一次

先把基准调好的增益写回代码，作为比较的共同起点。每次修改物理参数都重启程序，先用同样增益跑 B 的阶跃，再用 Tuner 重新调参。

| 工况 | 质量 | 减速比 | 其他参数 |
|---|---:|---:|---|
| 基准 | 8 kg | 10:1 | 不变 |
| 加重 | 12 kg | 10:1 | 不变 |
| 换减速器 | 8 kg | 5:1 | 不变 |

减速比必须修改 `kGearRatio`，让物理模型与编码器换算一起变化；**不要只改 Tuner 的反馈比值来冒充更换减速器**。比较实际高度曲线，不能直接比较转子圈数曲线。说明质量、减速比如何影响悬停电压、速度和动态响应。

## 4. 提交内容与评分

提交一个文件夹或仓库提交，包含：

- 修改后的 `Robot.java`，包含基准工况最终增益；不要提交 build、.gradle、ctre_sim。
- Tuner 配置导出文件或 Slot 0 截图。
- 三组基准调参曲线，以及两种变更工况的“原增益 / 重调后”曲线。
- 下表与不超过一页的分析；标明每次实验的质量、减速比、kG/kP/kD、是否达标。

| 工况/轮次 | kG | kP | kD | 最大稳定误差(m) | 到 ±0.02m 时间(s) | 最大超调(%) | 达标/现象 |
|---|---:|---:|---:|---:|---:|---:|---|
| 基准-初调 | | | | | | | |
| 基准-最终 | | | | | | | |
| 12kg-原增益 | | | | | | | |
| 12kg-重调 | | | | | | | |
| 5:1-原增益 | | | | | | | |
| 5:1-重调 | | | | | | | |

评分：连接与单位说明 15%；kG 实验 20%；位置闭环过程与基准表现 35%；质量/减速比对比分析 20%；配置与复现材料 10%。

回答三个问题：① kG 和 kP 分别解决什么问题？② 同一套增益为什么不一定适合不同质量/减速比？③ 哪些真实机构现象在本模型里没有体现？

## 5. 常见问题

- **看不到电机：** 确认程序仍在运行、没有配置失败输出、Tuner 指向 localhost:1250；检查 Tuner 的本机连接权限。不要点面向 roboRIO 的部署/临时诊断服务器来替代仿真。
- **电梯不动：** 初始增益为零本来就不会跟随目标；检查是否 Teleop Enabled、OpenLoop 是否误开、修改是否应用到 ID 1 Slot 0。
- **重启后参数没了：** 启动代码会覆盖增益，这是本作业明确的行为。先导出并写回代码。
- **曲线总是顶到底：** 先检查米/转子圈换算、输出正负号、增益和是否碰限位，不要改变模型来凑成绩。
- `src/main/java/frc/robot/sim` 中的通用电机仿真是原仓库遗留文件，当前 Robot **不调用**它；本题只有 `ElevatorSim` 在驱动物理状态，不能再叠加另一套仿真。

## 6. 验证与资料

本地完整检查：`./gradlew test build`；桌面仿真：`./gradlew simulateJava`。单元测试检查目标限幅与非有限输入处理；它不能替代 Tuner 连通性和闭环动态的人工验收。

本次编辑环境只有 JRE、没有 Java 编译器，`test` 在 `compileJava` 的工具链检查处停止；另一轮 `test build` 还遇到依赖仓库连接失败。因此**未声称编译通过，也未在该环境完成 Tuner X 图形界面联调**。请在 WPILib 开发环境执行以上命令后使用。

参考：[CTRE 仿真](https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/simulation/simulation-intro.html) · [Tuner 连接](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/connecting.html) · [闭环与重力前馈](https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/device-specific/talonfx/closed-loop-requests.html)
