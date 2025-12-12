package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.util.Array2c;
import fionathemortal.betterbiomeblend.common.util.Array3c;
import fionathemortal.betterbiomeblend.common.util.Array3i;
import fionathemortal.betterbiomeblend.common.util.Color;

public final class ColorBlending
{
    private static void
    blendColorsInLine(BlendContext blendContext, BlendConfig blendConfig, int sampleIndexY, int sampleIndexZ)
    {
        int lineFirst = Array3c.getArrayIndex(
            blendContext.sampleCountX,
            blendContext.sampleCountY,
            0,
            sampleIndexY,
            sampleIndexZ);

        int lowerFilter = blendContext.blockMinX - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinX + blendConfig.blendRadius;

        float sumR = 0;
        float sumG = 0;
        float sumB = 0;

        for (int block = lowerFilter;
             block < upperFilter;
             ++block)
        {
            int sampleIndexX = blendConfig.getSampleFromBlock(block) - blendContext.sampleMinX;

            int sampleIndex = lineFirst + Array3c.ELEMENT_SIZE * sampleIndexX;

            float sampleR = blendContext.samples[sampleIndex    ];
            float sampleG = blendContext.samples[sampleIndex + 1];
            float sampleB = blendContext.samples[sampleIndex + 2];

            sumR += sampleR;
            sumG += sampleG;
            sumB += sampleB;
        }

        int lineRingBufferIndex = (sampleIndexY % blendContext.lineCount);

        int outputDimX = blendContext.outputDimX;

        int outputIndexY = Array2c.getArrayIndex(outputDimX, 0, lineRingBufferIndex);

        for (int x = 0;
             x < outputDimX;
             ++x)
        {
            int upperSampleIndexX = blendConfig.getSampleFromBlock(upperFilter) - blendContext.sampleMinX;
            int upperSampleIndex  = lineFirst + Array3c.ELEMENT_SIZE * upperSampleIndexX;

            float upperSampleR = blendContext.samples[upperSampleIndex    ];
            float upperSampleG = blendContext.samples[upperSampleIndex + 1];
            float upperSampleB = blendContext.samples[upperSampleIndex + 2];

            sumR += upperSampleR;
            sumG += upperSampleG;
            sumB += upperSampleB;

            int outputIndex = outputIndexY + Array2c.ELEMENT_SIZE * x;

            blendContext.lineBuffer[outputIndex    ] = sumR;
            blendContext.lineBuffer[outputIndex + 1] = sumG;
            blendContext.lineBuffer[outputIndex + 2] = sumB;

            int lowerSampleIndexX = blendConfig.getSampleFromBlock(lowerFilter) - blendContext.sampleMinX;
            int lowerSampleIndex  = lineFirst + Array2c.ELEMENT_SIZE * lowerSampleIndexX;

            float lowerSampleR = blendContext.samples[lowerSampleIndex    ];
            float lowerSampleG = blendContext.samples[lowerSampleIndex + 1];
            float lowerSampleB = blendContext.samples[lowerSampleIndex + 2];

            sumR -= lowerSampleR;
            sumG -= lowerSampleG;
            sumB -= lowerSampleB;

            ++upperFilter;
            ++lowerFilter;
        }
    }

    private static void
    accumulateLine(BlendContext blendContext, int lineIndex, int blockCount)
    {
        int dimX = blendContext.outputDimX;

        int line = Array2c.getArrayIndex(dimX, 0, lineIndex);

        for (int index = 0;
             index < Array2c.ELEMENT_SIZE * dimX;
             ++index)
        {
            blendContext.lineSum[index] += blockCount * blendContext.lineBuffer[line + index];
        }
    }

    private static int
    initLineBuffers(BlendContext blendContext, int planeIndex)
    {
        BlendConfig blendConfig = blendContext.blendConfig;

        int lowerFilter = blendContext.blockMinY - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinY + blendConfig.blendRadius;

        int prevSampleIndexY = Integer.MIN_VALUE;

        int blockCount = 0;

        for (int y = lowerFilter;
             y < upperFilter;
             ++y, ++blockCount)
        {
            int sampleIndexY = blendConfig.getSampleFromBlock(y) - blendContext.sampleMinY;

            if (sampleIndexY != prevSampleIndexY)
            {
                if (prevSampleIndexY != Integer.MIN_VALUE)
                {
                    accumulateLine(blendContext, sampleIndexY, blockCount);
                }

                blendColorsInLine(blendContext, blendConfig, sampleIndexY, planeIndex);

                prevSampleIndexY = sampleIndexY;

                blockCount = 0;
            }
        }

        if (blockCount > 0)
        {
            accumulateLine(blendContext, prevSampleIndexY, blockCount);
        }

        return prevSampleIndexY;
    }

    private static void
    blendColorsInPlane(BlendContext blendContext, BlendConfig blendConfig, int sampleIndexZ)
    {
        int prevLineIndex = initLineBuffers(blendContext, sampleIndexZ);

        int outputDimX = blendContext.outputDimX;
        int outputDimY = blendContext.outputDimY;

        int planeRingBufferIndex = (sampleIndexZ % blendContext.planeCount);
        int planeRingBufferFirst = Array3c.getArrayIndex(outputDimX, outputDimY, 0, 0, planeRingBufferIndex);

        int lowerFilter = blendContext.blockMinY - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinY + blendConfig.blendRadius;

        for (int y = 0;
             y < outputDimY;
             ++y)
        {
            int upperLineIndexY = blendConfig.getSampleFromBlock(upperFilter) - blendContext.sampleMinY;
            int lowerLineIndexY = blendConfig.getSampleFromBlock(lowerFilter) - blendContext.sampleMinY;

            if (upperLineIndexY != prevLineIndex)
            {
                blendColorsInLine(blendContext, blendConfig, upperLineIndexY, sampleIndexZ);

                prevLineIndex = upperLineIndexY;
            }

            int upperLineFirst = Array2c.getArrayIndex(outputDimX, 0, upperLineIndexY % blendContext.lineCount);
            int lowerLineFirst = Array2c.getArrayIndex(outputDimX, 0, lowerLineIndexY % blendContext.lineCount);

            int outputIndex = planeRingBufferFirst + Array2c.getArrayIndex(outputDimX, 0, y);
            int sampleIndex = 0;

            for (int x = 0;
                 x < outputDimX;
                 ++x)
            {
                int upperSampleIndex = upperLineFirst + sampleIndex;
                int lowerSampleIndex = lowerLineFirst + sampleIndex;

                float colorR = blendContext.lineSum[sampleIndex    ];
                float colorG = blendContext.lineSum[sampleIndex + 1];
                float colorB = blendContext.lineSum[sampleIndex + 2];

                colorR += blendContext.lineBuffer[upperSampleIndex    ];
                colorG += blendContext.lineBuffer[upperSampleIndex + 1];
                colorB += blendContext.lineBuffer[upperSampleIndex + 2];

                blendContext.planeBuffer[outputIndex    ] = colorR;
                blendContext.planeBuffer[outputIndex + 1] = colorG;
                blendContext.planeBuffer[outputIndex + 2] = colorB;

                colorR -= blendContext.lineBuffer[lowerSampleIndex    ];
                colorG -= blendContext.lineBuffer[lowerSampleIndex + 1];
                colorB -= blendContext.lineBuffer[lowerSampleIndex + 2];

                blendContext.lineSum[sampleIndex    ] = colorR;
                blendContext.lineSum[sampleIndex + 1] = colorG;
                blendContext.lineSum[sampleIndex + 2] = colorB;

                outputIndex += Array3c.ELEMENT_SIZE;
                sampleIndex += Array2c.ELEMENT_SIZE;
            }

            ++upperFilter;
            ++lowerFilter;
        }
    }

    private static void
    accumulatePlane(BlendContext blendContext, int planeIndex, int blockCount)
    {
        int dimX = blendContext.outputDimX;
        int dimY = blendContext.outputDimY;

        int plane = Array3c.getArrayIndex(dimX, dimY, 0, 0, planeIndex);

        for (int index = 0;
             index < 3 * dimX * dimY;
             ++index)
        {
            blendContext.planeSum[index] += blockCount * blendContext.planeBuffer[plane + index];
        }
    }

    private static int
    initPlaneBuffers(BlendContext blendContext)
    {
        BlendConfig blendConfig = blendContext.blendConfig;

        int lowerFilter = blendContext.blockMinZ - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinZ + blendConfig.blendRadius;

        int prevSampleIndexZ = Integer.MIN_VALUE;

        int blockCount = 0;

        for (int z = lowerFilter;
             z < upperFilter;
             ++z, ++blockCount)
        {
            int sampleIndexZ = blendConfig.getSampleFromBlock(z) - blendContext.sampleMinZ;

            if (sampleIndexZ != prevSampleIndexZ)
            {
                if (prevSampleIndexZ != Integer.MIN_VALUE)
                {
                    accumulatePlane(blendContext, sampleIndexZ, blockCount);
                }

                blendColorsInPlane(blendContext, blendConfig, sampleIndexZ);

                prevSampleIndexZ = sampleIndexZ;

                blockCount = 0;
            }
        }

        if (blockCount > 0)
        {
            accumulatePlane(blendContext, prevSampleIndexZ, blockCount);
        }

        return prevSampleIndexZ;
    }

    public static void
    blendColors(BlendContext blendContext)
    {
        int prevSampleIndexZ = initPlaneBuffers(blendContext);

        int[] output = blendContext.output;

        int outputMinX = blendContext.outputMinX;
        int outputMinY = blendContext.outputMinY;
        int outputMinZ = blendContext.outputMinZ;

        int outputDimX = blendContext.outputDimX;
        int outputDimY = blendContext.outputDimY;
        int outputDimZ = blendContext.outputDimZ;

        int outputArrayDimX = blendContext.outputArrayDimX;
        int outputArrayDimY = blendContext.outputArrayDimY;

        int outputStrideX = Array3i.getStrideX();
        int outputStrideY = Array3i.getStrideY(outputArrayDimX);

        BlendConfig blendConfig = blendContext.blendConfig;

        int lowerFilter = blendContext.blockMinZ - blendConfig.blendRadius;
        int upperFilter = blendContext.blockMinZ + blendConfig.blendRadius;

        float filterMultiplier = blendContext.filterMultiplier;

        for (int z = 0;
             z < outputDimZ;
             ++z)
        {
            int upperSampleIndexZ = blendConfig.getSampleFromBlock(upperFilter) - blendContext.sampleMinZ;
            int lowerSampleIndexZ = blendConfig.getSampleFromBlock(lowerFilter) - blendContext.sampleMinZ;

            if (upperSampleIndexZ != prevSampleIndexZ)
            {
                blendColorsInPlane(blendContext, blendConfig, upperSampleIndexZ);

                prevSampleIndexZ = upperSampleIndexZ;
            }

            int upperPlaneIndex = (upperSampleIndexZ % blendContext.planeCount);
            int lowerPlaneIndex = (lowerSampleIndexZ % blendContext.planeCount);

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

                    float colorR = blendContext.planeSum[sampleIndex    ];
                    float colorG = blendContext.planeSum[sampleIndex + 1];
                    float colorB = blendContext.planeSum[sampleIndex + 2];

                    colorR += blendContext.planeBuffer[upperSampleIndex    ];
                    colorG += blendContext.planeBuffer[upperSampleIndex + 1];
                    colorB += blendContext.planeBuffer[upperSampleIndex + 2];

                    float filteredR = filterMultiplier * colorR;
                    float filteredG = filterMultiplier * colorG;
                    float filteredB = filterMultiplier * colorB;

                    output[outputIndex] = Color.OKLabsTosRGBAInt(filteredR, filteredG, filteredB);

                    colorR -= blendContext.planeBuffer[lowerSampleIndex    ];
                    colorG -= blendContext.planeBuffer[lowerSampleIndex + 1];
                    colorB -= blendContext.planeBuffer[lowerSampleIndex + 2];

                    blendContext.planeSum[sampleIndex    ] = colorR;
                    blendContext.planeSum[sampleIndex + 1] = colorG;
                    blendContext.planeSum[sampleIndex + 2] = colorB;

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
