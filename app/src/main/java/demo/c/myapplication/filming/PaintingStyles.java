package demo.c.myapplication.filming;

import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.vision.base.FeatureModelID;

public class PaintingStyles {

    public static FritzOnDeviceModel BICENTENNIAL_PRINT = new FritzOnDeviceModel(
            "bicentennial_print_640x480_015_small_optimized.pb",
            FeatureModelID.BICENTENNIAL_PRINT_MODEL_ID, 1);
    public static FritzOnDeviceModel FEMMES = new FritzOnDeviceModel(
            "femmes_640x480_015_small_optimized.pb",
            FeatureModelID.FEMMES_MODEL_ID, 1);
    public static FritzOnDeviceModel HEAD_OF_CLOWN = new FritzOnDeviceModel(
            "head_of_clown_640x480_015_small_optimized.pb",
            FeatureModelID.HEAD_OF_CLOWN_MODEL_ID, 1);
    public static FritzOnDeviceModel HORSES_ON_SEASHORE = new FritzOnDeviceModel(
            "horses_on_seashore_640x480_015_small_optimized.pb",
            FeatureModelID.HORSES_ON_SEASHORE_MODEL_ID, 1);
    public static FritzOnDeviceModel KALEIDOSCOPE = new FritzOnDeviceModel(
            "kaleidoscope_640x480_015_small_optimized.pb",
            FeatureModelID.KALEIDOSCOPE_MODEL_ID, 1);
    public static FritzOnDeviceModel PINK_BLUE_RHOMBUS = new FritzOnDeviceModel(
            "pink_blue_rhombus_640x480_015_small_optimized.pb",
            FeatureModelID.PINK_BLUE_RHOMBUS_MODEL_ID, 1
    );
    public static FritzOnDeviceModel POPPY_FIELD = new FritzOnDeviceModel(
            "poppy_field_640x480_015_small_optimized.pb",
            FeatureModelID.POPPY_FIELD_MODEL_ID, 1);
    public static FritzOnDeviceModel RITMO_PLASTICO = new FritzOnDeviceModel(
            "ritmo_plastico_640x480_015_small_optimized.pb",
            FeatureModelID.RITMO_PLASTICO_MODEL_ID,
            1);
    public static FritzOnDeviceModel STARRY_NIGHT = new FritzOnDeviceModel(
            "starry_night_640x480_015_small_optimized.pb",
            FeatureModelID.STARRY_NIGHT_MODEL_ID,
            1);
    public static FritzOnDeviceModel THE_SCREAM = new FritzOnDeviceModel(
            "the_scream_640x480_015_small_optimized.pb",
            FeatureModelID.THE_SCREAM_MODEL_ID,
            1);
    public static FritzOnDeviceModel THE_TRAIL = new FritzOnDeviceModel(
            "the_trial_640x480_015_small_optimized.pb",
            FeatureModelID.THE_TRAIL_MODEL_ID,
            1);


    public static FritzOnDeviceModel[] getAll() {
        return new FritzOnDeviceModel[]{

                BICENTENNIAL_PRINT,
                FEMMES,
                HEAD_OF_CLOWN,
                HORSES_ON_SEASHORE,
                KALEIDOSCOPE,
                PINK_BLUE_RHOMBUS,
                POPPY_FIELD,
                RITMO_PLASTICO,
                STARRY_NIGHT,
                THE_SCREAM,
                THE_TRAIL

        };
    }
}
