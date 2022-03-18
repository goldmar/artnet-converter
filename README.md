# artnet-converter
This CLI tool simulates an Art-Net node, receives DMX data, and forwards it to another node after transformation. Currently, the only supported transformation converts a value between 0 and 255 to a value between A and B for a given channel while maintaining the same position in the range.

## Usage
Build a standalone JAR:
```
./gradlew shadowJar
```

Run it:
```
java -jar ./build/libs/artnet-converter-1.0-SNAPSHOT-all.jar -- [ip-address-to-listen-on] [ip-address-to-forward-to] [channel1]:[min1]:[min1] [channel2]:[min2]:[min2]
```

Example:
```
java -jar ./build/libs/artnet-converter-1.0-SNAPSHOT-all.jar -- 192.168.0.10 192.168.0.20 5:128:255 6:128:255
```

## About
This library is based on [artnet4j](https://github.com/cansik/artnet4j).
