# android-sensor
A helper library for working with sensors on Android. This repository also includes some widgets and feature solution based on sensors.

## Inspiration
- `:core`: Logic was inspired and copied from this [repository](https://github.com/majidgolshadi/Android-Orientation-Sensor)
- `:widgets:card`: The idea was inspired by this [thread](https://www.reddit.com/r/reactnative/comments/w59tsg/fake_depth_animation_implemented_with_reanimated/)

## Installation
### build.gradle (project level)
This library is available on `mavenCentral`, so make sure to add this repository:
```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
}
```

### build.gradle (module level)
In our gradle script of our library or application we need to add the required dependencies:
```kotlin
dependencies {
    // Implement your own solutions depending on the core logic
    implementation("com.attafitamim.sensor:core:$version")
   
    // Use ready widgets and feature solutions
    implementation("com.attafitamim.sensor:card:$version")
}
```
> Check release notes for the latest version at [here](https://github.com/tamimattafi/android-sensor/releases)

## Usage
### Core Logic
Using the core logic is quite stright forward, create an instance of the desired sensor:
```kotlin
val sensor = OrientationSensor(context, ::tryUpdateValues)
```

Handle sensor's lifecycle:
```kotlin
// Turn the sensor on and off
sensor.on()
sensor.off()

// Dispose orientation sensor
sensor.dispose()

// Or force distruction
sensor.forceDispose()
```

Passing listeners has many ways in the Kotlin syntax:
- Use method reference
```kotlin
fun tryUpdateValues(azimuth: Double, pitch: Double, roll: Double) {
    // Use these values
}

val sensor = OrientationSensor(context, ::tryUpdateValues)
```

- Use anonymous objects
```kotlin
val listener = OrientationSensor.Delegate { azimuth, pitch, roll -> 
    // Use these values
}

val sensor = OrientationSensor(context, listener)
```

- Use classes
```kotlin
class Listener : OrientationSensor.Delegate {
    override fun onOrientation(azimuth: Double, pitch: Double, roll: Double) {
        // Use Values
    }
}

val sensor = OrientationSensor(context, Listener())
```

### Widgets: Card
Currently, controlling this widget is only available through XML. Passing src drawables and bitmaps is still available through code.
```xml
<com.attafitamim.sensor.widgets.card.SensibleImageCardView
    android:id="@+id/imgCard"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sensibleElement="all"
    app:shadowRadius="25"
    app:shadowBlurSampling="2"
    app:shadowColorFilter="#A8A8A8"
    app:shadowPadding="14dp"
    app:shadowAlphaPercent="0.9"
    app:sensorStabilizingLevel="100"
    android:padding="24dp"
    android:adjustViewBounds="true"
    android:src="@drawable/card"/>
```
> Check the [sample](https://github.com/tamimattafi/android-sensor/blob/main/sample/src/main/res/layout/activity_main.xml) for more information

## Supported attributes
1. `android:padding` - The default padding attribute, but it is very important in our case, it adds space for the view to be animated and move inside the frame, no space means no movement
- Available values: Dimension, dimension reference
- Default: `0dp` **(Note that there is no movement by default, this must be increased by the developer to meet their needs)**
2. `app:sensibleElement` - Selects the moving elements by the sensor
- Available options: `all`, `shadow`, `image`, `none`
- Default: `all`
3. `app:shadowRadius` - Defines the radius of the shadow (Or the blur applied to it)
- Available values: `Integer`, min = `1`, max = `25` (If you need more radius increase sampling)
- Default: `25`
4. `app:shadowBlurSampling` - how many times blur is going to be applied to the image
- Available values: `Integer`, min = `1`, max = As long as performance isn't affected
- Default: `1`
5. `app:shadowColorFilter` - Dark filter color to apply to the shadow
- Available values: Color reference, hex
- Default: `0xFF7F7F7F`
6. `app:shadowPadding` - Makes the shadow smaller than the actual image to add more realism
- Available values: Dimension, dimension reference
- Default: `0dp`
7. `app:shadowAlphaPercent` - Controls the opacity of the shadow
- Available values: Float, preferable min = `0.01` (1% opaque), max = `1` (100% opaque)
- Default: `1` (100%)
8. `app:sensorStabilizingLevel` - The amount of values ignored by the sensor before saving the initial state
Since values given by android sensor upon initialization might be a little random, it's better to ignore some of them before actually starting to animate the view
- Available values: Integer, min = `0` (No values to be ignored), max = as long as the UX isn't affected
- Default: `100`

## Licence
Apache License 2.0
A permissive license whose main conditions require preservation of copyright and license notices. Contributors provide an express grant of patent rights. Licensed works, modifications, and larger works may be distributed under different terms and without source code.

More information [here](https://github.com/tamimattafi/android-sensor/blob/main/LICENSE)
