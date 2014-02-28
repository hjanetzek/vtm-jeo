### Experimental Jeo layers for VTM

```
export ANDROID_HOME=/path/to/android-sdk
```

#### Run the Android example
```
./gradlew :vtm-jeo-android:run
```

#### Run the Desktop example
```
./gradlew :vtm-jeo-desktop:run
```


#### Setup Eclipse project (clone 'vtm-jeo' in 'vtm' directory):
```
git clone --recursive https://github.com/hjanetzek/vtm
cd vtm
./gradlew eclipse

git clone https://github.com/hjanetzek/vtm-jeo
cd vtm-jeo
./gradlew eclipse

```

Then import 'vtm' folder as 'existing projects'
