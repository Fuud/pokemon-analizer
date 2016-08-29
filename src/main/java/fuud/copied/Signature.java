package fuud.copied;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import POGOProtos.Networking.Envelopes.SignatureOuterClass;
import POGOProtos.Networking.Envelopes.Unknown6OuterClass;
import POGOProtos.Networking.Envelopes.Unknown6OuterClass.Unknown6.Unknown2;
import POGOProtos.Networking.Requests.RequestOuterClass;

import com.google.protobuf.ByteString;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.device.ActivityStatus;
import com.pokegoapi.api.device.DeviceInfo;
import com.pokegoapi.api.device.LocationFixes;
import com.pokegoapi.api.device.SensorInfo;

import com.pokegoapi.util.Constant;
import com.pokegoapi.util.Crypto;
import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.util.Random;

public class Signature {

    
    public static void setSignature(long startTime, byte[] sessionHash, long seed, RequestEnvelopeOuterClass.RequestEnvelope.Builder builder) {
        if (builder.getAuthTicket() == null) {
            //System.out.println("Ticket == null");
            return;
        }

        long currentTime = System.currentTimeMillis();

        byte[] authTicketBA = builder.getAuthTicket().toByteArray();

		/*
			Todo : reuse this later when we know the input
			byte[] unknown = "b8fa9757195897aae92c53dbcf8a60fb3d86d745".getBytes();
			XXHashFactory factory = XXHashFactory.safeInstance();
			StreamingXXHash64 xx64 = factory.newStreamingHash64(0x88533787);
			xx64.update(unknown, 0, unknown.length);
			long unknown25 = xx64.getValue();
		*/

        Random random = new Random();

        SignatureOuterClass.Signature.Builder sigBuilder = SignatureOuterClass.Signature.newBuilder()
                .setLocationHash1(getLocationHash1(authTicketBA))
                .setLocationHash2(getLocationHash2())
                .setSessionHash(ByteString.copyFrom(sessionHash))
                .setTimestamp(System.currentTimeMillis())
                .setTimestampSinceStart(currentTime - startTime)
                .setDeviceInfo(getDefault(seed).getDeviceInfo())
                .setActivityStatus(getDefaultActivityStatus(random))
//                .addAllLocationFix(LocationFixes.getDefault(api, builder, currentTime, random))
                .setUnknown25(Constant.UNK25);

//        SignatureOuterClass.Signature.SensorInfo sensorInfo = createSensorInfo(currentTime, random);
//        if (sensorInfo != null) {
//            sigBuilder.setSensorInfo(sensorInfo);
//        }

        for (RequestOuterClass.Request serverRequest : builder.getRequestsList()) {
            byte[] request = serverRequest.toByteArray();
            sigBuilder.addRequestHash(getRequestHash(authTicketBA, request));
        }

        // TODO: Call encrypt function on this
        byte[] uk2 = sigBuilder.build().toByteArray();
        byte[] iv = new byte[32];
        new Random().nextBytes(iv);
        byte[] encrypted = Crypto.encrypt(uk2, iv).toByteBuffer().array();
        Unknown6OuterClass.Unknown6 uk6 = Unknown6OuterClass.Unknown6.newBuilder()
                .setRequestType(6)
                .setUnknown2(Unknown2.newBuilder().setEncryptedSignature(ByteString.copyFrom(encrypted))).build();
        builder.addUnknown6(uk6);
    }

    private static byte[] getBytes(double input) {
        long rawDouble = Double.doubleToRawLongBits(input);
        return new byte[]{
                (byte) (rawDouble >>> 56),
                (byte) (rawDouble >>> 48),
                (byte) (rawDouble >>> 40),
                (byte) (rawDouble >>> 32),
                (byte) (rawDouble >>> 24),
                (byte) (rawDouble >>> 16),
                (byte) (rawDouble >>> 8),
                (byte) rawDouble
        };
    }

    private static int getLocationHash1(byte[] authTicket) {
        XXHashFactory factory = XXHashFactory.safeInstance();
        StreamingXXHash32 xx32 = factory.newStreamingHash32(0x1B845238);
        xx32.update(authTicket, 0, authTicket.length);
        byte[] bytes = new byte[8 * 3];

        System.arraycopy(getBytes(0), 0, bytes, 0, 8);
        System.arraycopy(getBytes(0), 0, bytes, 8, 8);
        System.arraycopy(getBytes(0), 0, bytes, 16, 8);

        xx32 = factory.newStreamingHash32(xx32.getValue());
        xx32.update(bytes, 0, bytes.length);
        return xx32.getValue();
    }

    private static int getLocationHash2() {
        XXHashFactory factory = XXHashFactory.safeInstance();
        byte[] bytes = new byte[8 * 3];

        System.arraycopy(getBytes(0), 0, bytes, 0, 8);
        System.arraycopy(getBytes(0), 0, bytes, 8, 8);
        System.arraycopy(getBytes(0), 0, bytes, 16, 8);

        StreamingXXHash32 xx32 = factory.newStreamingHash32(0x1B845238);
        xx32.update(bytes, 0, bytes.length);

        return xx32.getValue();
    }

    private static long getRequestHash(byte[] authTicket, byte[] request) {
        XXHashFactory factory = XXHashFactory.safeInstance();
        StreamingXXHash64 xx64 = factory.newStreamingHash64(0x1B845238);
        xx64.update(authTicket, 0, authTicket.length);
        xx64 = factory.newStreamingHash64(xx64.getValue());
        xx64.update(request, 0, request.length);
        return xx64.getValue();
    }

    public static DeviceInfo getDefault(long seed) {
        DeviceInfo deviceInfo = new DeviceInfo();
        Random random = new Random(seed);
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        deviceInfo.setDeviceId(bytesToHex(bytes));
        String[][] devices =
                {
                        {"iPad3,1", "iPad", "J1AP"},
                        {"iPad3,2", "iPad", "J2AP"},
                        {"iPad3,3", "iPad", "J2AAP"},
                        {"iPad3,4", "iPad", "P101AP"},
                        {"iPad3,5", "iPad", "P102AP"},
                        {"iPad3,6", "iPad", "P103AP"},

                        {"iPad4,1", "iPad", "J71AP"},
                        {"iPad4,2", "iPad", "J72AP"},
                        {"iPad4,3", "iPad", "J73AP"},
                        {"iPad4,4", "iPad", "J85AP"},
                        {"iPad4,5", "iPad", "J86AP"},
                        {"iPad4,6", "iPad", "J87AP"},
                        {"iPad4,7", "iPad", "J85mAP"},
                        {"iPad4,8", "iPad", "J86mAP"},
                        {"iPad4,9", "iPad", "J87mAP"},

                        {"iPad5,1", "iPad", "J96AP"},
                        {"iPad5,2", "iPad", "J97AP"},
                        {"iPad5,3", "iPad", "J81AP"},
                        {"iPad5,4", "iPad", "J82AP"},

                        {"iPad6,7", "iPad", "J98aAP"},
                        {"iPad6,8", "iPad", "J99aAP"},

                        {"iPhone5,1", "iPhone", "N41AP"},
                        {"iPhone5,2", "iPhone", "N42AP"},
                        {"iPhone5,3", "iPhone", "N48AP"},
                        {"iPhone5,4", "iPhone", "N49AP"},

                        {"iPhone6,1", "iPhone", "N51AP"},
                        {"iPhone6,2", "iPhone", "N53AP"},

                        {"iPhone7,1", "iPhone", "N56AP"},
                        {"iPhone7,2", "iPhone", "N61AP"},

                        {"iPhone8,1", "iPhone", "N71AP"}

                };
        String[] osVersions = {"8.1.1", "8.1.2", "8.1.3", "8.2", "8.3", "8.4", "8.4.1",
                "9.0", "9.0.1", "9.0.2", "9.1", "9.2", "9.2.1", "9.3", "9.3.1", "9.3.2", "9.3.3", "9.3.4"};
        deviceInfo.setFirmwareType(osVersions[random.nextInt(osVersions.length)]);
        String[] device = devices[random.nextInt(devices.length)];
        deviceInfo.setDeviceModelBoot(device[0]);
        deviceInfo.setDeviceModel(device[1]);
        deviceInfo.setHardwareModel(device[2]);
        deviceInfo.setFirmwareBrand("iPhone OS");
        deviceInfo.setDeviceBrand("Apple");
        deviceInfo.setHardwareManufacturer("Apple");
        return deviceInfo;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int var = bytes[index] & 0xFF;
            hexChars[index * 2] = hexArray[var >>> 4];
            hexChars[index * 2 + 1] = hexArray[var & 0x0F];
        }
        return new String(hexChars).toLowerCase();
    }

    public static SignatureOuterClass.Signature.ActivityStatus getDefaultActivityStatus(Random random) {
        boolean tilting = random.nextInt() % 2 == 0;
        ActivityStatus activityStatus =  new ActivityStatus();
        activityStatus.setStationary(true);
        if (tilting) {
            activityStatus.setTilting(true);
        }
        return activityStatus.getActivityStatus();
    }
}