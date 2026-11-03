#!/usr/bin/env python3
# Usage: python3 scripts/seamless_tile.py <square photo crop> <out.webp>  (docs/tata-slings/textures.md)
"""Make each fabric photo a true repeating tile: blend it with its half-offset copy, so the
edges are the photo's own middle (continuous across the wrap) and the middle stays the photo."""
import sys, numpy as np
from PIL import Image
src, dst = sys.argv[1], sys.argv[2]
# A thin border off first: photo edges carry folds and lens falloff that would sit on the seam.
_img = Image.open(src).convert('RGB')
_b = _img.size[0] // 20
_img = _img.crop((_b, _b, _img.size[0] - _b, _img.size[1] - _b)).resize(_img.size, Image.LANCZOS)
im = np.asarray(_img).astype(np.float64)
h, w, _ = im.shape
# Flatten the photo's light (a lamp's falloff, a fold's shadow): divide by a very soft blur of
# itself, so tiles don't show as lighter and darker blocks. The weave and the print stay.
from PIL import ImageFilter
soft = np.asarray(_img.filter(ImageFilter.GaussianBlur(w / 6))).astype(np.float64)
im = im / np.maximum(soft, 1) * soft.reshape(-1, 3).mean(axis=0)
rolled = np.roll(np.roll(im, h // 2, axis=0), w // 2, axis=1)
def ramp(n, band):
    d = np.minimum(np.arange(n), np.arange(n)[::-1]) / (n * band)
    t = np.clip(d, 0, 1)
    return t * t * (3 - 2 * t)  # smoothstep
band = float(sys.argv[3]) if len(sys.argv) > 3 else 0.25
m = np.outer(ramp(h, band), ramp(w, band))[..., None]
out = m * im + (1 - m) * rolled
Image.fromarray(np.clip(out + 0.5, 0, 255).astype(np.uint8)).save(dst, 'WEBP', quality=78, method=6)
