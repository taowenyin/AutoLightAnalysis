package siso.edu.cn.autolightanalysis;

import java.util.ArrayList;

public class Algorithm {

    // 包装类型
    public enum PackingType {
        PE,
        CPP,
        PA,
        PET,
        PA_PE,
        PA_CPP,
        PET_CPP,
        PET_PE
    }

    static public double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }

    // 无包装的算法
    public static class NoPacking {
        /**
         * 牛肉预测
         * @param r_525　为525nm处反射率
         * @param r_545　为545nm处反射率
         * @param r_565　为565nm处反射率
         * @param r_572　为572nm处反射率
         * @return　array [脱氧肌红蛋白, 氧合肌红蛋白, 高铁肌红蛋白]
         */
        static public double[] beefPrediction(double r_525, double r_545, double r_565, double r_572) {

            double a0_525 = log(1 / r_525, 10);
            double a1_545 = log(1 / r_545, 10);
            double a2_565 = log(1 / r_565, 10);
            double a3_572 = log(1 / r_572, 10);

            double x1 = a1_545 / a0_525;
            double x2 = a2_565 / a0_525;
            double x3 = a3_572 / a0_525;

            // 脱氧肌红蛋白
            double dmb = (-0.51 + 4.16 * x1 + 3.03 * x2 - 6.55 * x3) * 100;
            // 氧合肌红蛋白
            double omb = (-1.32 - 13.44 * x1 - 18.37 * x2 + 32.71 * x3) * 100;
            // 高铁肌红蛋白
            double mmb = (2.88 + 9.53 * x1 + 15.85 * x2 - 27.00 * x3) * 100;

            return new double[] {dmb, omb, mmb};
        }

        /**
         * 猪肉预测
         * @param r_525　为525nm处反射率
         * @param r_545　为545nm处反射率
         * @param r_565　为565nm处反射率
         * @param r_572　为572nm处反射率
         * @return　array [脱氧肌红蛋白, 氧合肌红蛋白, 高铁肌红蛋白]
         */
        static public double[] porkPrediction(double r_525, double r_545, double r_565, double r_572) {

            double a0_525 = log(1 / r_525, 10);
            double a1_545 = log(1 / r_545, 10);
            double a2_565 = log(1 / r_565, 10);
            double a3_572 = log(1 / r_572, 10);

            double x1 = a1_545 / a0_525;
            double x2 = a2_565 / a0_525;
            double x3 = a3_572 / a0_525;

            // 脱氧肌红蛋白
            double dmb = (0.45 - 0.35 * x1 + 0.12 * x2 - 0.15 * x3) * 100;
            // 氧合肌红蛋白
            double omb = (0.11 - 2.74 * x1 - 0.00081 * x2 + 2.91 * x3) * 100;
            // 高铁肌红蛋白
            double mmb = (0.22 + 3.96 * x1 - 0.15 * x2 - 3.85 * x3) * 100;

            return new double[] {dmb, omb, mmb};
        }
    }

    // 有包装的算法
    public static class Packing {
        /**
         * 牛肉预测
         * @param r_525　为525nm处反射率
         * @param r_545　为545nm处反射率
         * @param r_565　为565nm处反射率
         * @param r_572　为572nm处反射率
         * @param type enum 包装类型
         * @return array [脱氧肌红蛋白, 氧合肌红蛋白, 高铁肌红蛋白]
         */
        static public double[] beefPrediction(double r_525, double r_545, double r_565, double r_572, PackingType type) {

            double s = 0;

            switch (type) {
                case PE:
                    s = 6.5;
                    break;
                case CPP:
                    s = 3;
                    break;
                case PA:
                    s = 7;
                    break;
                case PET:
                    s = 8;
                    break;
                case PA_PE:
                    s = 12.5;
                    break;
                case PA_CPP:
                    s = 14.5;
                    break;
                case PET_CPP:
                    s = 14;
                    break;
                case PET_PE:
                    s = 13;
                    break;
            }

            double a0_525 = log(1 / (r_525 + s), 10);
            double a1_545 = log(1 / (r_545 + s), 10);
            double a2_565 = log(1 / (r_565 + s), 10);
            double a3_572 = log(1 / (r_572 + s), 10);

            double x1 = a1_545 / a0_525;
            double x2 = a2_565 / a0_525;
            double x3 = a3_572 / a0_525;

            // 脱氧肌红蛋白
            double dmb = (-0.51 + 4.16 * x1 + 3.03 * x2 - 6.55 * x3) * 100;
            // 氧合肌红蛋白
            double omb = (-1.32 - 13.44 * x1 - 18.37 * x2 + 32.71 * x3) * 100;
            // 高铁肌红蛋白
            double mmb = (2.88 + 9.53 * x1 + 15.85 * x2 - 27.00 * x3) * 100;

            return new double[] {dmb, omb, mmb};
        }
    }

}
