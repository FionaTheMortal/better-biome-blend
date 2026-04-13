package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.util.*;

import java.util.Arrays;

public final class ColorBlending
{
    private static int
    getSampleContribution(ColorConfig blendConfig, int blockMin, int blockMax, int sample)
    {
        int sampleMin = blendConfig.getBlockFromSample(sample);
        int sampleMax = sampleMin + blendConfig.sampleSize;

        int overlapMin = Math.max(sampleMin, blockMin);
        int overlapMax = Math.min(sampleMax, blockMax);

        int contribution = overlapMax - overlapMin;

        int result = Math.max(0, contribution);

        return result;
    }

    private static void
    copyLineForBlending(ColorGenContext context, int sampleIndexY, int sampleIndexZ)
    {
        int lineFirst = Array3i.getArrayIndex(
            context.sampleCountX,
            context.sampleCountY,
            0,
            sampleIndexY,
            sampleIndexZ);

        int dstIndex = 0;
        int srcIndex = lineFirst;

        for (int x = 0;
            x < context.sampleCountX;
            ++x)
        {
            int color = context.samples[srcIndex];

            Color.sRGBByteToOKLabs(color, context.lineSamples, dstIndex);

            srcIndex += 1;
            dstIndex += Array3c.ELEMENT_SIZE;
        }
    }

    private static void
    blendColorsInLine(ColorGenContext context, ColorConfig blendConfig, int sampleIndexY, int sampleIndexZ)
    {
        copyLineForBlending(context, sampleIndexY, sampleIndexZ);

        int lowerFilter = context.blockMinX - blendConfig.blendRadius;
        int upperFilter = context.blockMinX + blendConfig.blendRadius;

        float sumR = 0;
        float sumG = 0;
        float sumB = 0;

        final int lowerFilterSample = blendConfig.floorBlockToSample(lowerFilter);
        final int upperFilterSample = blendConfig.ceilBlockToSample(upperFilter);

        for (int sampleX = lowerFilterSample;
            sampleX < upperFilterSample;
            ++sampleX)
        {
            int sampleIndex = sampleX - context.sampleMinX;
            int sampleFirst = Array1c.getArrayIndex(sampleIndex);

            float sampleR = context.lineSamples[sampleFirst    ];
            float sampleG = context.lineSamples[sampleFirst + 1];
            float sampleB = context.lineSamples[sampleFirst + 2];

            float contribution = getSampleContribution(blendConfig, lowerFilter, upperFilter, sampleX);

            sumR += sampleR * contribution;
            sumG += sampleG * contribution;
            sumB += sampleB * contribution;
        }

        final int outputRingBufferIndex = (sampleIndexY % context.lineCount);
        final int outputDim = context.outputDimX;
        final int outputLineBase = Array2c.getArrayIndex(outputDim, 0, outputRingBufferIndex);

        for (int x = 0;
             x < outputDim;
             ++x)
        {
            int upperSampleIndex = blendConfig.floorBlockToSample(upperFilter) - context.sampleMinX;
            int lowerSampleIndex = blendConfig.floorBlockToSample(lowerFilter) - context.sampleMinX;

            int upperSampleFirst = Array1c.getArrayIndex(upperSampleIndex);
            int lowerSampleFirst = Array1c.getArrayIndex(lowerSampleIndex);

            float upperSampleR = context.lineSamples[upperSampleFirst    ];
            float upperSampleG = context.lineSamples[upperSampleFirst + 1];
            float upperSampleB = context.lineSamples[upperSampleFirst + 2];

            sumR += upperSampleR;
            sumG += upperSampleG;
            sumB += upperSampleB;

            int outputIndex = outputLineBase + Array1c.getArrayIndex(x);

            context.lineBuffer[outputIndex    ] = sumR;
            context.lineBuffer[outputIndex + 1] = sumG;
            context.lineBuffer[outputIndex + 2] = sumB;

            float lowerSampleR = context.lineSamples[lowerSampleFirst    ];
            float lowerSampleG = context.lineSamples[lowerSampleFirst + 1];
            float lowerSampleB = context.lineSamples[lowerSampleFirst + 2];

            sumR -= lowerSampleR;
            sumG -= lowerSampleG;
            sumB -= lowerSampleB;

            ++upperFilter;
            ++lowerFilter;
        }
    }

    private static void
    accumulateLine(ColorGenContext colorGenContext, int lineIndex, int blockCount)
    {
        final int dimX = colorGenContext.outputDimX;
        final int line = Array2c.getArrayIndex(dimX, 0, lineIndex);

        for (int index = 0;
             index < Array1c.ELEMENT_SIZE * dimX;
             ++index)
        {
            colorGenContext.lineSum[index] += blockCount * colorGenContext.lineBuffer[line + index];
        }
    }

    private static int
    initLineBuffers(ColorGenContext colorGenContext, int planeIndex)
    {
        ColorConfig blendConfig = colorGenContext.blendConfig;

        Arrays.fill(colorGenContext.lineSum, 0.0f);

        int lowerFilter = colorGenContext.blockMinY - blendConfig.blendRadius;
        int upperFilter = colorGenContext.blockMinY + blendConfig.blendRadius;

        int lowerFilterSample = blendConfig.floorBlockToSample(lowerFilter);
        int upperFilterSample = blendConfig.ceilBlockToSample(upperFilter);

        int lastSampleIndexY = Integer.MIN_VALUE;

        for (int sampleY = lowerFilterSample;
            sampleY < upperFilterSample;
            ++sampleY)
        {
            int sampleIndexY = sampleY - colorGenContext.sampleMinY;
            int contribution = getSampleContribution(blendConfig, lowerFilter, upperFilter, sampleY);

            blendColorsInLine(colorGenContext, blendConfig, sampleIndexY, planeIndex);
            accumulateLine(colorGenContext, sampleIndexY, contribution);

            lastSampleIndexY = sampleIndexY;
        }

        return lastSampleIndexY;
    }

    private static void
    blendColorsInPlane(ColorGenContext colorGenContext, ColorConfig blendConfig, int sampleIndexZ)
    {
        int prevLineIndex = initLineBuffers(colorGenContext, sampleIndexZ);

        int outputDimX = colorGenContext.outputDimX;
        int outputDimY = colorGenContext.outputDimY;

        int planeRingBufferIndex = (sampleIndexZ % colorGenContext.planeCount);
        int planeRingBufferFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, planeRingBufferIndex);

        int lowerFilter = colorGenContext.blockMinY - blendConfig.blendRadius;
        int upperFilter = colorGenContext.blockMinY + blendConfig.blendRadius;

        for (int y = 0;
             y < outputDimY;
             ++y)
        {
            int upperLineIndexY = blendConfig.floorBlockToSample(upperFilter) - colorGenContext.sampleMinY;
            int lowerLineIndexY = blendConfig.floorBlockToSample(lowerFilter) - colorGenContext.sampleMinY;

            if (upperLineIndexY != prevLineIndex)
            {
                blendColorsInLine(colorGenContext, blendConfig, upperLineIndexY, sampleIndexZ);

                prevLineIndex = upperLineIndexY;
            }

            int upperLineFirst = Array2c.getArrayIndex(outputDimX, 0, upperLineIndexY % colorGenContext.lineCount);
            int lowerLineFirst = Array2c.getArrayIndex(outputDimX, 0, lowerLineIndexY % colorGenContext.lineCount);

            int outputIndex = planeRingBufferFirst + Array2c.getArrayIndex(outputDimX, 0, y);
            int sampleIndex = 0;

            for (int x = 0;
                 x < outputDimX;
                 ++x)
            {
                int upperSampleIndex = upperLineFirst + sampleIndex;
                int lowerSampleIndex = lowerLineFirst + sampleIndex;

                float colorR = colorGenContext.lineSum[sampleIndex    ];
                float colorG = colorGenContext.lineSum[sampleIndex + 1];
                float colorB = colorGenContext.lineSum[sampleIndex + 2];

                colorR += colorGenContext.lineBuffer[upperSampleIndex    ];
                colorG += colorGenContext.lineBuffer[upperSampleIndex + 1];
                colorB += colorGenContext.lineBuffer[upperSampleIndex + 2];

                colorGenContext.planeBuffer[outputIndex    ] = colorR;
                colorGenContext.planeBuffer[outputIndex + 1] = colorG;
                colorGenContext.planeBuffer[outputIndex + 2] = colorB;

                colorR -= colorGenContext.lineBuffer[lowerSampleIndex    ];
                colorG -= colorGenContext.lineBuffer[lowerSampleIndex + 1];
                colorB -= colorGenContext.lineBuffer[lowerSampleIndex + 2];

                colorGenContext.lineSum[sampleIndex    ] = colorR;
                colorGenContext.lineSum[sampleIndex + 1] = colorG;
                colorGenContext.lineSum[sampleIndex + 2] = colorB;

                outputIndex += Array3c.ELEMENT_SIZE;
                sampleIndex += Array2c.ELEMENT_SIZE;
            }

            ++upperFilter;
            ++lowerFilter;
        }
    }

    private static void
    accumulatePlane(ColorGenContext colorGenContext, int planeIndex, int blockCount)
    {
        int dimX = colorGenContext.outputDimX;
        int dimY = colorGenContext.outputDimY;

        int plane = Array3c.getArrayIndex(dimX, dimY, 0, 0, planeIndex);

        for (int index = 0;
             index < 3 * dimX * dimY;
             ++index)
        {
            colorGenContext.planeSum[index] += blockCount * colorGenContext.planeBuffer[plane + index];
        }
    }

    private static int
    initPlaneBuffers(ColorGenContext context)
    {
        ColorConfig config = context.blendConfig;

        Arrays.fill(context.planeSum, 0.0f);

        int lowerFilter = context.blockMinZ - config.blendRadius;
        int upperFilter = context.blockMinZ + config.blendRadius;

        int lowerFilterSample = config.floorBlockToSample(lowerFilter);
        int upperFilterSample = config.ceilBlockToSample(upperFilter);

        int lastSampleIndexZ = Integer.MIN_VALUE;

        for (int sampleZ = lowerFilterSample;
             sampleZ < upperFilterSample;
             ++sampleZ)
        {
            int sampleIndexZ = sampleZ - context.sampleMinZ;
            int contribution = getSampleContribution(config, lowerFilter, upperFilter, sampleZ);

            blendColorsInPlane(context, config, sampleIndexZ);
            accumulatePlane(context, sampleIndexZ, contribution);

            lastSampleIndexZ = sampleIndexZ;
        }

        return lastSampleIndexZ;
    }

    public static void
    blendColors(ColorGenContext colorGenContext)
    {
        int prevSampleIndexZ = initPlaneBuffers(colorGenContext);

        int[] output = colorGenContext.output;

        int outputMinX = colorGenContext.outputMinX;
        int outputMinY = colorGenContext.outputMinY;
        int outputMinZ = colorGenContext.outputMinZ;

        int outputDimX = colorGenContext.outputDimX;
        int outputDimY = colorGenContext.outputDimY;
        int outputDimZ = colorGenContext.outputDimZ;

        int outputArrayDimX = colorGenContext.outputArrayDimX;
        int outputArrayDimY = colorGenContext.outputArrayDimY;

        int outputStrideX = Array3i.getStrideX();
        int outputStrideY = Array3i.getStrideY(outputArrayDimX);

        ColorConfig blendConfig = colorGenContext.blendConfig;

        int lowerFilter = colorGenContext.blockMinZ - blendConfig.blendRadius;
        int upperFilter = colorGenContext.blockMinZ + blendConfig.blendRadius;

        float filterMultiplier = colorGenContext.filterMultiplier;

        for (int z = 0;
             z < outputDimZ;
             ++z)
        {
            int upperSampleIndexZ = blendConfig.floorBlockToSample(upperFilter) - colorGenContext.sampleMinZ;
            int lowerSampleIndexZ = blendConfig.floorBlockToSample(lowerFilter) - colorGenContext.sampleMinZ;

            if (upperSampleIndexZ != prevSampleIndexZ)
            {
                blendColorsInPlane(colorGenContext, blendConfig, upperSampleIndexZ);

                prevSampleIndexZ = upperSampleIndexZ;
            }

            int upperPlaneIndex = (upperSampleIndexZ % colorGenContext.planeCount);
            int lowerPlaneIndex = (lowerSampleIndexZ % colorGenContext.planeCount);

            int upperPlaneFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, upperPlaneIndex);
            int lowerPlaneFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, lowerPlaneIndex);

            int outputIndexY = Array3i.getArrayIndex(outputArrayDimX, outputArrayDimY, outputMinX, outputMinY, outputMinZ + z);

            int sampleIndex = 0;

            for (int y = 0;
                 y < outputDimY;
                 ++y)
            {
                int outputIndex = outputIndexY;

                for (int x = 0;
                     x < outputDimX;
                     ++x)
                {
                    int upperSampleIndex = upperPlaneFirst + sampleIndex;
                    int lowerSampleIndex = lowerPlaneFirst + sampleIndex;

                    float colorR = colorGenContext.planeSum[sampleIndex    ];
                    float colorG = colorGenContext.planeSum[sampleIndex + 1];
                    float colorB = colorGenContext.planeSum[sampleIndex + 2];

                    colorR += colorGenContext.planeBuffer[upperSampleIndex    ];
                    colorG += colorGenContext.planeBuffer[upperSampleIndex + 1];
                    colorB += colorGenContext.planeBuffer[upperSampleIndex + 2];

                    float filteredR = filterMultiplier * colorR;
                    float filteredG = filterMultiplier * colorG;
                    float filteredB = filterMultiplier * colorB;

                    output[outputIndex] = Color.OKLabsTosRGBAInt(filteredR, filteredG, filteredB);

                    colorR -= colorGenContext.planeBuffer[lowerSampleIndex    ];
                    colorG -= colorGenContext.planeBuffer[lowerSampleIndex + 1];
                    colorB -= colorGenContext.planeBuffer[lowerSampleIndex + 2];

                    colorGenContext.planeSum[sampleIndex    ] = colorR;
                    colorGenContext.planeSum[sampleIndex + 1] = colorG;
                    colorGenContext.planeSum[sampleIndex + 2] = colorB;

                    outputIndex += outputStrideX;
                    sampleIndex += Array3c.ELEMENT_SIZE;
                }

                outputIndexY += outputStrideY;
            }

            ++upperFilter;
            ++lowerFilter;
        }
    }
}
