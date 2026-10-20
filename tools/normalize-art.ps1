$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceImage = [Drawing.Image]::FromFile((Join-Path $projectRoot 'art\fried-rice-source.png'))
$canvas = [Drawing.Bitmap]::new(64,64,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [Drawing.Graphics]::FromImage($canvas)
try {
    $graphics.Clear([Drawing.Color]::Transparent)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
    $scale = [Math]::Min(64.0/$sourceImage.Width,64.0/$sourceImage.Height)
    $spriteWidth = [int][Math]::Round($sourceImage.Width*$scale)
    $spriteHeight = [int][Math]::Round($sourceImage.Height*$scale)
    $rect = [Drawing.Rectangle]::new([int]((64-$spriteWidth)/2),[int]((64-$spriteHeight)/2),$spriteWidth,$spriteHeight)
    $graphics.DrawImage($sourceImage,$rect,0,0,$sourceImage.Width,$sourceImage.Height,[Drawing.GraphicsUnit]::Pixel)
    $canvas.Save((Join-Path $projectRoot 'src\main\resources\assets\smoke_kitchen\textures\item\fried_rice.png'),[Drawing.Imaging.ImageFormat]::Png)
} finally { $graphics.Dispose(); $canvas.Dispose(); $sourceImage.Dispose() }
Write-Output 'Exported generated sprite as a transparent 64x64 game texture.'
