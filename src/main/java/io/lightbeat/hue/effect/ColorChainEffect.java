package io.lightbeat.hue.effect;

import io.lightbeat.config.Config;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Passes one color to all lights one by one and selects a new one after all have the new color.
 * Keeps the order of the lights and always overwrites other color based effects.
 */
public class ColorChainEffect extends AbstractThresholdEffect {

    private static final double ADDITIONAL_LIGHT_PROBABILITY = 0.2d;

    private List<Light> lightsInOrder;

    private Color currentColor;
    private Color currentFadeColor;
    private int currentIndex;


    public ColorChainEffect(Config config, ColorSet colorSet, double brightnessThreshold, double activationProbability) {
        super(config, colorSet, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {
        lightsInOrder = new ArrayList<>();
        currentIndex = Integer.MAX_VALUE;
        currentColor = null;
    }

    @Override
    void execute() {

        if (lightsInOrder.isEmpty()) {

            for (Light light : lightUpdate.getLights()) {
                if (light.getColorController().setControllingEffect(this)) {
                    light.getColorController().undoColorChange(this);
                    lightsInOrder.add(light);
                }
            }

            if (lightsInOrder.isEmpty()) {
                return;
            }
        }

        boolean isFirstLight = true;
        while (isFirstLight || Math.random() < ADDITIONAL_LIGHT_PROBABILITY) {

            isFirstLight = false;
            if (currentIndex++ >= lightsInOrder.size() - 1) {
                currentColor = currentColor != null ? currentFadeColor : colorSet.getNextColor();
                currentFadeColor = colorSet.getNextColor(currentColor);
                currentIndex = 0;
            }

            Light nextLight = lightsInOrder.get(currentIndex);
            nextLight.getColorController().setColor(this, currentColor);
            nextLight.getColorController().setFadeColor(this, currentFadeColor);
        }
    }

    @Override
    public void executionDone() {
        for (Light light : lightsInOrder) {
            light.getColorController().setFadeColor(this, currentFadeColor);
            light.getColorController().unsetControllingEffect(this);
        }
    }
}
