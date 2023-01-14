// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import static frc.robot.Constants.*;

import com.ctre.phoenix.sensors.Pigeon2;
import com.swervedrivespecialties.swervelib.Mk4SwerveModuleHelper;
import com.swervedrivespecialties.swervelib.SdsModuleConfigurations;
import com.swervedrivespecialties.swervelib.SwerveModule;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

public class DrivetrainSubsystem extends SubsystemBase {
        /**
         * The maximum voltage that will be delivered to the drive motors.
         * <p>
         * This can be reduced to cap the robot's maximum speed. Typically, this is
         * useful during initial testing of the robot.
         */
        public static final double MAX_VOLTAGE = 12.0;
        // DONE Measure the drivetrain's maximum velocity or calculate the theoretical.
        // The formula for calculating the theoretical maximum velocity is:
        // <Motor free speed RPM> / 60 * <Drive reduction> * <Wheel diameter meters> *
        // pi
        // By default this value is setup for a Mk3 standard module using Falcon500s to
        // drive.
        // An example of this constant for a Mk4 L2 module with NEOs to drive is:
        // 5880.0 / 60.0 / SdsModuleConfigurations.MK4_L2.getDriveReduction() *
        // SdsModuleConfigurations.MK4_L2.getWheelDiameter() * Math.PI
        /**
         * The maximum velocity of the robot in meters per second.
         * <p>
         * This is a measure of how fast the robot should be able to drive in a straight
         * line.
         */
        public static final double MAX_VELOCITY_METERS_PER_SECOND = 6380.0 / 60.0 *
                        SdsModuleConfigurations.MK4_L1.getDriveReduction() *
                        SdsModuleConfigurations.MK4_L1.getWheelDiameter() * Math.PI;
        public static final double MAX_VELOCITY_METERS_PER_SECOND_TRAJECTORY = 6380.0 / 60.0 *
                        SdsModuleConfigurations.MK4_L1.getDriveReduction() *
                        SdsModuleConfigurations.MK4_L1.getWheelDiameter() * Math.PI;

        // Max acceleration needed for TrajectoryConfig constructor. Using value from 0
        // to Autonomous example
        public static final double MAX_ACCELERATION_METERS_PER_SECOND_SQUARED = 3.0; //original line of code
        // public static final double MAX_ACCELERATION_METERS_PER_SECOND_SQUARED = 1.5;

        /**
         * The maximum angular velocity of the robot in radians per second.
         * <p>
         * This is a measure of how fast the robot can rotate in place.
         */
        // Here we calculate the theoretical maximum angular velocity. You can also
        // replace this with a measured amount.
        public static final double MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND = MAX_VELOCITY_METERS_PER_SECOND /
                        Math.hypot(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0);

        private final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
                        // Front left
                        new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Front right
                        new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Back left
                        new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Back right
                        new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0));

        // By default we use a Pigeon for our gyroscope. But if you use another
        // gyroscope, like a NavX, you can change this.
        // The important thing about how you configure your gyroscope is that rotating
        // the robot counter-clockwise should
        // cause the angle reading to increase until it wraps back over to zero.
        // Don't Remove following if you are using a Pigeon
        // private final PigeonIMU m_pigeon = new PigeonIMU(DRIVETRAIN_PIGEON_ID);
        Pigeon2 m_pigeon = new Pigeon2(DRIVETRAIN_PIGEON_ID);
        Double m_pigeonPitch;
        Double m_pigeonYaw;
        Double m_pigeonRoll;
        // Uncomment following if you are using a NavX
        // private final AHRS m_navx = new AHRS(SPI.Port.kMXP, (byte) 200); // NavX
        // connected over MXP

        private final SwerveDriveOdometry odometery = new SwerveDriveOdometry(Constants.kDriveKinematics,
        new Rotation2d(0));

        // Field object to keep track of location of bot. 
        private final Field2d field = new Field2d();

        // These are our modules. We initialize them in the constructor.
        public SwerveModule m_frontLeftModule;
        public SwerveModule m_frontRightModule;
        public SwerveModule m_backLeftModule;
        public SwerveModule m_backRightModule;

        private ChassisSpeeds m_chassisSpeeds = new ChassisSpeeds(0.0, 0.0, 0.0);

        private SwerveModuleState[] states;

        public DrivetrainSubsystem() {
                ShuffleboardTab tab = Shuffleboard.getTab("Drivetrain");
                SmartDashboard.putData("Field", field);

                // There are 4 methods you can call to create your swerve modules.
                // The method you use depends on what motors you are using.
                //
                // Mk3SwerveModuleHelper.createFalcon500(...)
                // Your module has two Falcon 500s on it. One for steering and one for driving.
                //
                // Mk3SwerveModuleHelper.createNeo(...)
                // Your module has two NEOs on it. One for steering and one for driving.
                //
                // Mk3SwerveModuleHelper.createFalcon500Neo(...)
                // Your module has a Falcon 500 and a NEO on it. The Falcon 500 is for driving
                // and the NEO is for steering.
                //
                // Mk3SwerveModuleHelper.createNeoFalcon500(...)
                // Your module has a NEO and a Falcon 500 on it. The NEO is for driving and the
                // Falcon 500 is for steering.
                //
                // Similar helpers also exist for Mk4 modules using the Mk4SwerveModuleHelper
                // class.

                // By default we will use Falcon 500s in standard configuration. But if you use
                // a different configuration or motors
                // you MUST change it. If you do not, your code will crash on startup.
                // Setup motor configuration DONE DONE DONE
                // Shuffleboard.putBoolean("Gyro Angle", m_pigeon.getAbsoluteCompassHeading());

                m_frontLeftModule = Mk4SwerveModuleHelper.createFalcon500(
                                // This parameter is optional, but will allow you to see the current state of
                                // the module on the dashboard.
                                tab.getLayout("Front Left Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(0, 0),
                                // This can either be STANDARD or FAST depending on your gear configuration
                                Mk4SwerveModuleHelper.GearRatio.L1,
                                // This is the ID of the drive motor
                                FRONT_LEFT_MODULE_DRIVE_MOTOR,
                                // This is the ID of the steer motor
                                FRONT_LEFT_MODULE_STEER_MOTOR,
                                // This is the ID of the steer encoder
                                FRONT_LEFT_MODULE_STEER_ENCODER,
                                // This is how much the steer encoder is offset from true zero (In our case,
                                // zero is facing straight forward)
                                FRONT_LEFT_MODULE_STEER_OFFSET);

                // We will do the same for the other modules
                m_frontRightModule = Mk4SwerveModuleHelper.createFalcon500(
                                tab.getLayout("Front Right Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(2, 0),
                                Mk4SwerveModuleHelper.GearRatio.L1,
                                FRONT_RIGHT_MODULE_DRIVE_MOTOR,
                                FRONT_RIGHT_MODULE_STEER_MOTOR,
                                FRONT_RIGHT_MODULE_STEER_ENCODER,
                                FRONT_RIGHT_MODULE_STEER_OFFSET);

                m_backLeftModule = Mk4SwerveModuleHelper.createFalcon500(
                                tab.getLayout("Back Left Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(4, 0),
                                Mk4SwerveModuleHelper.GearRatio.L1,
                                BACK_LEFT_MODULE_DRIVE_MOTOR,
                                BACK_LEFT_MODULE_STEER_MOTOR,
                                BACK_LEFT_MODULE_STEER_ENCODER,
                                BACK_LEFT_MODULE_STEER_OFFSET);

                m_backRightModule = Mk4SwerveModuleHelper.createFalcon500(
                                tab.getLayout("Back Right Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(6, 0),
                                Mk4SwerveModuleHelper.GearRatio.L1,
                                BACK_RIGHT_MODULE_DRIVE_MOTOR,
                                BACK_RIGHT_MODULE_STEER_MOTOR,
                                BACK_RIGHT_MODULE_STEER_ENCODER,
                                BACK_RIGHT_MODULE_STEER_OFFSET);
        }

        /**
         * Sets the gyroscope angle to zero. This can be used to set the direction the
         * robot is currently facing to the
         * 'forwards' direction.
         */
        public void zeroGyroscope() {
                // Don't Remove following if you are using a Pigeon
                // m_pigeon.setFusedHeading(0.0);
                m_pigeon.setYaw(0.0);

                // Uncomment Following if you are using a NavX
                // m_navx.zeroYaw();
        }

        public Rotation2d getGyroscopeRotation() {
                // Don't Remove Follwoing if you are using a Pigeon
                // return Rotation2d.fromDegrees(m_pigeon.getFusedHeading());
                return Rotation2d.fromDegrees(m_pigeon.getYaw());

                // Uncomment following if you are using a NavX
                // if (m_navx.isMagnetometerCalibrated()) {
                // // We will only get valid fused headings if the magnetometer is calibrated
                // return Rotation2d.fromDegrees(m_navx.getFusedHeading());
                // }
                //
                // // We have to invert the angle of the NavX so that rotating the robot
                // counter-clockwise makes the angle increase.
                // return Rotation2d.fromDegrees(360.0 - m_navx.getYaw());
        }
        public Rotation2d getGyroscopePitch() {
            return Rotation2d.fromDegrees(m_pigeon.getPitch());
        }

        public Rotation2d getGyroscopeRoll() {
            return Rotation2d.fromDegrees(m_pigeon.getRoll());
        }

        public void drive(ChassisSpeeds chassisSpeeds) {
                m_chassisSpeeds = chassisSpeeds;
                states = m_kinematics.toSwerveModuleStates(m_chassisSpeeds);
        }

        /**
         * Drive method that can take in SwerveModuleState array. Needed for auton since
         * we have to provide a consumer of SwerveModuleState[]
         * 
         * @param states the states of the swerve modules
         */
        public void drive(SwerveModuleState[] states) {
                this.states = states;
        }

        @Override
        public void periodic() {
           m_pigeonPitch = m_pigeon.getPitch();
           m_pigeonYaw = m_pigeon.getYaw();
           m_pigeonRoll = m_pigeon.getRoll();
            SmartDashboard.putNumber("Robot Pitch", m_pigeonPitch);
            SmartDashboard.putNumber("Robot Yaw", m_pigeonYaw);
            SmartDashboard.putNumber("Robot Roll", m_pigeonRoll);
            SmartDashboard.updateValues();
                // Moving this line into the drive() method
                // SwerveModuleState[] states =
                // m_kinematics.toSwerveModuleStates(m_chassisSpeeds);

                // Trying update robot position on field image on dashboard
                field.setRobotPose(odometery.getPoseMeters());

                if (states == null) {
                        return;
                }
                SwerveDriveKinematics.desaturateWheelSpeeds(states, MAX_VELOCITY_METERS_PER_SECOND);

                m_frontLeftModule.set(states[0].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                                states[0].angle.getRadians());
                m_frontRightModule.set(states[1].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                                states[1].angle.getRadians());
                m_backLeftModule.set(states[2].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                                states[2].angle.getRadians());
                m_backRightModule.set(states[3].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                                states[3].angle.getRadians());
        }

        /**
         * Gets the current pose of the robot
         * @return
         */
        public Pose2d getPose() {
                return odometery.getPoseMeters();
            }


            public void resetOdometry(Pose2d pose) {
                odometery.resetPosition(pose, getRotation2d());
            }

            public void stopModules() {
                m_frontLeftModule.set(0, m_frontLeftModule.getSteerAngle());
                m_frontRightModule.set(0, m_frontRightModule.getSteerAngle());
                m_backLeftModule.set(0, m_backLeftModule.getSteerAngle());
                m_backRightModule.set(0, m_backRightModule.getSteerAngle());
            }

            public double getHeading() {
                return Math.IEEEremainder(m_pigeon.getYaw(), 360);
            }
        
            public Rotation2d getRotation2d() {
                return Rotation2d.fromDegrees(getHeading());
            }
            public double getPitch() {
                return m_pigeon.getPitch();
            }
            public double getRoll() {
                return m_pigeon.getRoll();
            }
            public void driveStraightSlow() {
                m_frontLeftModule.set(2, 0);
                m_frontRightModule.set(2, 0);;
                m_backLeftModule.set(2, 0);;
                m_backRightModule.set(2, 0);
            }
            public void sturdyBase() {
                m_backRightModule.set(0, 45);;
                m_backLeftModule.set(0, -45);;
                m_frontRightModule.set(0, -45);;
                m_frontLeftModule.set(0, 45);;
            }
}
