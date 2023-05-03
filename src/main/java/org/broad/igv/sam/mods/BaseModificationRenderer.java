package org.broad.igv.sam.mods;

import org.broad.igv.sam.Alignment;
import org.broad.igv.sam.AlignmentBlock;
import org.broad.igv.sam.AlignmentTrack;

import java.awt.*;
import java.util.List;

public class BaseModificationRenderer {

    public static void drawModifications(
            Alignment alignment,
            double bpStart,
            double locScale,
            Rectangle rowRect,
            Graphics g,
            AlignmentTrack.ColorOption colorOption) {

        switch (colorOption) {
            case BASE_MODIFICATION_5MC:
                draw5mC(alignment, bpStart, locScale, rowRect, g, false);
                break;
            case BASE_MODIFICATION_C:
                draw5mC(alignment, bpStart, locScale, rowRect, g, true);
                break;
            case BASE_MODIFICATION_6MA:
                draw6mA(alignment, bpStart, locScale, rowRect, g);
                break;
            default:
                draw(alignment, bpStart, locScale, rowRect, g);
        }

    }

    /**
     * Helper function for AlignmentRenderer.  Draw base modifications over alignment.
     *
     * @param alignment
     * @param bpStart
     * @param locScale
     * @param rowRect
     * @param g
     */
    private static void draw(
            Alignment alignment,
            double bpStart,
            double locScale,
            Rectangle rowRect,
            Graphics g) {

        List<BaseModificationSet> baseModificationSets = alignment.getBaseModificationSets();

        if (baseModificationSets != null) {
            for (AlignmentBlock block : alignment.getAlignmentBlocks()) {
                // Compute bounds
                int pY = (int) rowRect.getY();
                int dY = (int) rowRect.getHeight();
                int dX = (int) Math.max(1, (1.0 / locScale));

                for (int i = block.getBases().startOffset; i < block.getBases().startOffset + block.getBases().length; i++) {

                    int blockIdx = i - block.getBases().startOffset;
                    int pX = (int) ((block.getStart() + blockIdx - bpStart) / locScale);

                    // Don't draw out of clipping rect
                    if (pX > rowRect.getMaxX()) {
                        break;
                    } else if (pX + dX < rowRect.getX()) {
                        continue;
                    }

                    // Search all sets for modifications of this base.  For now keeps mod with > probability
                    // TODO -- merge mods in some way
                    byte lh = 0;
                    String modification = null;
                    for (BaseModificationSet bmSet : baseModificationSets) {
                        if (bmSet.containsPosition(i)) {
                            if (modification == null || Byte.toUnsignedInt(bmSet.getLikelihoods().get(i)) > Byte.toUnsignedInt(lh)) {
                                modification = bmSet.getModification();
                                lh = bmSet.getLikelihoods().get(i);
                            }
                        }
                    }

                    if (modification != null) {

                        Color c = BaseModificationColors.getModColor(modification, lh, AlignmentTrack.ColorOption.BASE_MODIFICATION);
                        g.setColor(c);

                        // Expand narrow width to make more visible
                        if (dX < 3) {
                            dX = 3;
                            pX--;
                        }
                        g.fillRect(pX, pY, dX, Math.max(1, dY - 2));
                    }
                }
            }
        }
    }

    /**
     * Helper function for AlignmentRenderer.  Draw base modifications over alignment for "5mC" mode.
     * <p>
     * Notes:
     * Designed primarily for visualization of 5mC modifications compatible with existing bisulfite seq viz
     * - 5mC methylated bases colored red
     * - Non modified bases colored blue
     * - Other modificationc colored as defined in BaseModificationColors
     * <p>
     * If multiple modifications are specified for a base the modification with the highest probability is
     * drawn.
     *
     * @param alignment
     * @param bpStart
     * @param locScale
     * @param rowRect
     * @param g
     */
    private static void draw5mC(
            Alignment alignment,
            double bpStart,
            double locScale,
            Rectangle rowRect,
            Graphics g,
            boolean allMods) {

        List<BaseModificationSet> baseModificationSets = alignment.getBaseModificationSets();
        if (baseModificationSets != null) {

            for (AlignmentBlock block : alignment.getAlignmentBlocks()) {
                // Compute bounds
                int pY = (int) rowRect.getY();
                int dY = (int) rowRect.getHeight();
                int dX = (int) Math.max(1, (1.0 / locScale));

                for (int i = block.getBases().startOffset; i < block.getBases().startOffset + block.getBases().length; i++) {

                    int blockIdx = i - block.getBases().startOffset;
                    int pX = (int) ((block.getStart() + blockIdx - bpStart) / locScale);

                    // Don't draw out of clipping rect
                    if (pX > rowRect.getMaxX()) {
                        break;
                    } else if (pX + dX < rowRect.getX()) {
                        continue;
                    }

                    // Search all sets for modifications of this base, select modification with largest likelihood
                    int lh = -1;
                    String modification = null;

                    // Compare likelihoods, including likelihood of no modification
                    int noModificationLikelihood = 255;
                    for (BaseModificationSet bmSet : baseModificationSets) {

                        // This coloring mode is exclusively for "C" modifications, either 5mC or all C mods
                        if (bmSet.getCanonicalBase() != 'C') continue;
                        if (bmSet.getModification().equals("m") || allMods) {
                            if (bmSet.containsPosition(i)) {
                                int l = Byte.toUnsignedInt(bmSet.getLikelihoods().get(i));
                                noModificationLikelihood -= l;
                                if (modification == null || l > lh) {
                                    modification = bmSet.getModification();
                                    lh = l;
                                }
                            }
                        }
                    }

                    if (modification != null) {

                        Color c = noModificationLikelihood > lh ?
                                BaseModificationColors.getNoModColor((byte) noModificationLikelihood) :
                                BaseModificationColors.getModColor(modification, (byte) lh, AlignmentTrack.ColorOption.BASE_MODIFICATION_5MC);
                        g.setColor(c);

                        // Expand narrow width to make more visible
                        if (dX < 3) {
                            dX = 3;
                            pX--;
                        }
                        g.fillRect(pX, pY, dX, Math.max(1, dY - 2));
                    }
                }
            }
        }
    }

    /**
     * Helper function for AlignmentRenderer.  Draw base modifications over alignment for "6mA" mode.
     * <p>
     * If multiple modifications are specified for a base the modification with the highest probability is
     * drawn.
     *
     * @param alignment
     * @param bpStart
     * @param locScale
     * @param rowRect
     * @param g
     */
    private static void draw6mA(
            Alignment alignment,
            double bpStart,
            double locScale,
            Rectangle rowRect,
            Graphics g) {

        List<BaseModificationSet> baseModificationSets = alignment.getBaseModificationSets();
        if (baseModificationSets == null) { return; }

        for (AlignmentBlock block : alignment.getAlignmentBlocks()) {
            // Compute bounds
            int pY = (int) rowRect.getY();
            int dY = (int) rowRect.getHeight();
            int dX = (int) Math.max(1, (1.0 / locScale));

            for (int i = block.getBases().startOffset; i < block.getBases().startOffset + block.getBases().length; i++) {

                int blockIdx = i - block.getBases().startOffset;
                int pX = (int) ((block.getStart() + blockIdx - bpStart) / locScale);

                // Don't draw out of clipping rect
                if (pX > rowRect.getMaxX()) {
                    break;
                } else if (pX + dX < rowRect.getX()) {
                    continue;
                }

                // Search all sets for modifications of this base, select modification with largest likelihood
                int lh = -1;
                String modification = null;

                for (BaseModificationSet bmSet : baseModificationSets) {

                    if (bmSet.getCanonicalBase() != 'A' && bmSet.getCanonicalBase() != 'T') continue;
                    if (! bmSet.getModification().equals("a")) continue;

                    if (bmSet.containsPosition(i)) {
                        int l = Byte.toUnsignedInt(bmSet.getLikelihoods().get(i));
                        if (modification == null || l > lh) {
                            modification = bmSet.getModification();
                            lh = l;
                        }
                    }
                }

                if (modification != null) {
                    Color c = BaseModificationColors.getModColor(modification, (byte) lh, AlignmentTrack.ColorOption.BASE_MODIFICATION_6MA);
                    g.setColor(c);

                    // Expand narrow width to make more visible
                    if (dX < 3) {
                        dX = 3;
                        pX--;
                    }
                    g.fillRect(pX, pY, dX, Math.max(1, dY - 2));
                }
            }
        }
    }
}
