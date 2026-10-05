"""Render the actual Android vector cabinet as a static preview, not a device screenshot."""
from pathlib import Path
import xml.etree.ElementTree as ET
import subprocess

root = Path(__file__).resolve().parent
vector = ET.parse(root / "app/src/main/res/drawable/coco_monitor.xml").getroot()
namespace = "{http://schemas.android.com/apk/res/android}"
svg = ET.Element("svg", xmlns="http://www.w3.org/2000/svg", viewBox="0 0 1000 770")
ET.SubElement(svg, "rect", x="0", y="0", width="1000", height="770", fill="#080e13")
ET.SubElement(svg, "rect", x="95", y="127", width="660", height="495", fill="#2daf29")
for shape in vector:
    attributes = {"d": shape.get(namespace + "pathData"), "fill": shape.get(namespace + "fillColor", "none")}
    if shape.get(namespace + "strokeColor"):
        attributes["stroke"] = shape.get(namespace + "strokeColor")
        attributes["stroke-width"] = shape.get(namespace + "strokeWidth")
    ET.SubElement(svg, "path", attributes)
for text, x, y, color, size in [("03", 853, 151, "#d34b37", 28),
                                ("TRS-80", 879, 187, "#1f2527", 18),
                                ("COLOR VIDEO", 880, 213, "#c8cfc7", 13)]:
    ET.SubElement(svg, "text", x=str(x), y=str(y), fill=color,
                  **{"font-family": "sans-serif", "font-size": str(size),
                     "text-anchor": "middle"}).text = text
out = root / "build/previews/coco-monitor.png"
out.parent.mkdir(parents=True, exist_ok=True)
subprocess.run(["rsvg-convert", "--width=1000", "--height=770", "--output", str(out)],
               input=ET.tostring(svg), check=True)
print(out)
