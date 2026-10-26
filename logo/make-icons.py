#!/usr/bin/env python3
"""Generates every icon of the project from logo/Page 3.svg.

    python3 logo/make-icons.py            # needs rsvg-convert (librsvg)

Outputs:
  logo/saytypist.png                 1024 px, transparent wordmark (source)
  logo/saytypist-mark.png            512 px, the quotation-mark symbol
  desktop icons (png/ico, tray, window)
  Android launcher icons, adaptive icon layers and the Play Store icon
"""
from __future__ import annotations

import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
LOGO_SVG = ROOT / "logo" / "Page 3.svg"
TMP = ROOT / "build" / "logo"

BG = (251, 249, 247, 255)   # warm paper white used behind the logo
TEAL = (0, 128, 128, 255)
BLACK = (17, 17, 17, 255)


def render_svg(size: int) -> Image.Image:
    TMP.mkdir(parents=True, exist_ok=True)
    out = TMP / f"logo-{size}.png"
    subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size), str(LOGO_SVG), "-o", str(out)],
        check=True,
    )
    return Image.open(out).convert("RGBA")


def fit(image: Image.Image, size: int, canvas: int | None = None,
        color: tuple | None = None) -> Image.Image:
    """Scales `image` so it fits in size x size, keeps transparency."""
    bbox = image.split()[3].point(lambda a: 255 if a > 8 else 0).getbbox()
    cropped = image.crop(bbox)
    width, height = cropped.size
    scale = min(size / width, size / height)
    resized = cropped.resize(
        (max(1, round(width * scale)), max(1, round(height * scale))), Image.LANCZOS
    )
    if color is not None:
        solid = Image.new("RGBA", resized.size, color)
        solid.putalpha(resized.split()[3])
        resized = solid
    target = canvas or size
    out = Image.new("RGBA", (target, target), (0, 0, 0, 0))
    out.alpha_composite(resized, ((target - resized.width) // 2, (target - resized.height) // 2))
    return out


def rounded_background(size: int, radius_ratio: float = 0.22) -> Image.Image:
    base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(base)
    draw.rounded_rectangle(
        (0, 0, size - 1, size - 1), radius=int(size * radius_ratio), fill=BG
    )
    return base


def circle_background(size: int) -> Image.Image:
    base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(base).ellipse((0, 0, size - 1, size - 1), fill=BG)
    return base


def logo_on(size: int, inner_scale: float, circular: bool = False) -> Image.Image:
    canvas = circle_background(size) if circular else rounded_background(size)
    inner = int(size * inner_scale)
    mark = fit(wordmark, inner)
    canvas.alpha_composite(mark, ((size - mark.width) // 2, (size - mark.height) // 2))
    return canvas


def main() -> None:
    global wordmark
    source = render_svg(2048)
    wordmark = fit(source, 1024)                     # cropped, transparent
    wordmark.save(ROOT / "logo" / "saytypist.png")

    # quotation marks (the compact symbol of the wordmark)
    quotes = source.crop((1300 + 137, 200 + 67, 1300 + 617, 200 + 408))
    mark = fit(quotes, 512)
    mark.save(ROOT / "logo" / "saytypist-mark.png")

    # ---------------------------------------------------------------- desktop
    icons = ROOT / "desktop" / "icons"
    icons.mkdir(parents=True, exist_ok=True)
    fit(mark, 512).save(icons / "saytypist.png")
    fit(mark, 512).save(
        icons / "saytypist.ico",
        sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )

    res = ROOT / "desktop" / "src" / "main" / "resources" / "icons"
    res.mkdir(parents=True, exist_ok=True)
    fit(wordmark, 256).save(res / "saytypist.png")
    fit(mark, 128).save(res / "saytypist-mark.png")
    # tray icons should stay visible on light and dark panels
    fit(mark, 64, color=TEAL).save(res / "tray.png")

    # ---------------------------------------------------------------- Android
    android = ROOT / "app" / "src" / "main"
    densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for name, factor in densities.items():
        folder = android / "res" / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        size = round(48 * factor)
        logo_on(size, 0.80).save(folder / "ic_launcher.png")
        logo_on(size, 0.80, circular=True).save(folder / "ic_launcher_round.png")

        # adaptive icon layers: 108dp canvas, 66dp safe zone
        drawable = android / "res" / f"drawable-{name}"
        drawable.mkdir(parents=True, exist_ok=True)
        canvas = round(108 * factor)
        inner = round(60 * factor)
        foreground = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
        symbol = fit(mark, inner)
        foreground.alpha_composite(
            symbol, ((canvas - symbol.width) // 2, (canvas - symbol.height) // 2)
        )
        foreground.save(drawable / "ic_launcher_foreground.png")
        mono = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
        mono.alpha_composite(foreground)
        mono.save(drawable / "ic_launcher_monochrome.png")

    logo_on(512, 0.76).save(android / "ic_launcher-playstore.png")
    print("icons written")


if __name__ == "__main__":
    main()
