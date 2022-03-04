# 407ETRTest

## Use gradlew.bat on windows, instead of gradlew

### Building
```bash
./gradlew :buildJar
```

### Executing
You can execute it in interactive mode:
```bash
java -jar ./build/libs/407ETRTest-all-1.0-SNAPSHOT.jar
```

Or provide a start and end location and it will output the results:
```bash
java -jar ./build/libs/407ETRTest-all-1.0-SNAPSHOT.jar [start] [end]
```

You can also provide a different json data file:
```bash
java -jar ./build/libs/407ETRTest-all-1.0-SNAPSHOT.jar [start] [end] [datafile]
java -jar ./build/libs/407ETRTest-all-1.0-SNAPSHOT.jar [datafile]
```

 Example:
 ```bash
$ java -jar ./build/libs/407ETRTest-all-1.0-SNAPSHOT.jar "Salem Road" "QEW"
Distance: 115.277km
Cost: $28.82

$ java -jar ./build/libs/407ETRTest-all-1.0-SNAPSHOT.jar "QEW" "Highway 400"
Distance: 67.748km
Cost: $16.94
```

### Testing
```bash
./gradlew :test
```
