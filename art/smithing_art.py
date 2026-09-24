"""Editable, deliberately placed pixel art. One texel per model unit; no noise or resampling.

PNG files are delivery exports. Change this source, then run tools/generate_textures.py --write.
Model JSON in assets/immersive_smithing/models is directly editable in Blockbench (Java Block/Item).
"""
from PIL import Image, ImageDraw

IRON = ['#171c22', '#2a3037', '#3d4852', '#596773', '#87949b', '#b6bfb9']
WOOD = ['#292321', '#42332b', '#604735', '#806044', '#a08055', '#bea171']
BRONZE = ['#49352c', '#755039', '#a77d4f', '#d2ad6b']
HEAT = ['#562a28', '#913c2a', '#cf5c2b', '#f39136', '#ffd36b', '#fff0b4']
TIERS = {
    'stone': ['#272831', '#43454e', '#666d72', '#929990', '#c0c4ac'],
    'iron': ['#20282f', '#4a5861', '#7c909a', '#bdc9c7', '#f0eee0'],
    'diamond': ['#123c46', '#1b666e', '#339e9e', '#6bdbcc', '#c2f7e0'],
    'netherite': ['#231e28', '#3e333e', '#605260', '#88757e', '#b3a0a0'],
}

def canvas(size, color=(0,0,0,0)):
    im = Image.new('RGBA', size, color)
    return im, ImageDraw.Draw(im)

def sprite(rows, palette):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im, d = canvas((16,16))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != '.': d.point((x,y), fill=palette[key])
    return im

HAMMER = [
'................',
'......000.......',
'.....03440......',
'....0332240.....',
'....03222240....',
'.....02122240...',
'......01122240..',
'......bb012230..',
'.....bHh001230..',
'....bHho..010...',
'...oHho....0....',
'..oHho..........',
'.oHho...........',
'.ohho...........',
'..oo............',
'................']
TONGS = [
'................',
'...........000..',
'..........0340..',
'.........0320...',
'........0320.00.',
'.......0320.0340',
'.......020.0320.',
'......00b00320..',
'.....oHob2200...',
'....oHho000.....',
'...oHho.0ho.....',
'..oHho.0hho.....',
'.oHho.0hho......',
'.oho.0hho.......',
'..o..0ho........',
'......0.........']
WORKPIECE = [
'................',
'..........oo....',
'.........o54o...',
'........o5541o..',
'.......o55431o..',
'......o554310...',
'.....o554310....',
'....o554310.....',
'...o554310......',
'..o554310.......',
'..o44310........',
'...o310.........',
'....00..........',
'................',
'................',
'................']
LOADED = [
'................',
'................',
'............oo..',
'...........o54o.',
'..........o5431o',
'..........o431o.',
'...........o1o..',
'............o...',
'................',
'................',
'................',
'................',
'................',
'................',
'................',
'................']
BOOK = [
'................',
'...oooooooooo...',
'..ohhHHHHHHHbo..',
'..ohmhhhhhhHppo.',
'..ohmhhhhhbhpqo.',
'..ohmhbbbbhhpqo.',
'..ohmhbBBbBhpqo.',
'..ohmhhBBbhhpqo.',
'..ohmhhBbBhhpqo.',
'..ohmhBBBBhhpqo.',
'..ohmhhhhhhhpqo.',
'..ohmhbbbbhhppo.',
'..ohhHHHHHHHbo..',
'..ommppppppppo..',
'...oooooooooo...',
'................']

def textures():
    out = {}
    for tier, ramp in TIERS.items():
        palette = {str(i):v for i,v in enumerate(ramp)} | {'o':IRON[0], 'b':BRONZE[2], 'H':WOOD[4], 'h':WOOD[2]}
        hammer = sprite(HAMMER, palette)
        d = ImageDraw.Draw(hammer)
        # Material-specific details, not merely recolours: chipped stone, steel peen, inset jewel, bronze collars.
        if tier == 'stone':
            d.point((7,2), fill=ramp[1]); d.point((12,8), fill=ramp[1]); d.point((5,4), fill=ramp[0])
        if tier == 'diamond':
            d.rectangle((8,5,9,6), fill=ramp[0]); d.point((8,5), fill=ramp[4]); d.point((9,6), fill=ramp[3])
        if tier == 'netherite':
            d.line((6,3,12,9), fill=BRONZE[2]); d.point((12,9), fill=BRONZE[3]); d.point((3,12), fill=BRONZE[2])
        out[f'item/{tier}_smithing_hammer.png'] = hammer
        tongs = sprite(TONGS, palette)
        if tier == 'netherite':
            d = ImageDraw.Draw(tongs); d.point((11,3), fill=BRONZE[2]); d.point((13,6), fill=BRONZE[3])
        out[f'item/{tier}_smithing_tongs.png'] = tongs
    hot_palette = dict(zip('012345',HEAT)) | {'o': HEAT[0]}
    out['item/hot_workpiece.png'] = sprite(WORKPIECE, hot_palette)
    out['item/tongs_workpiece.png'] = sprite(LOADED, hot_palette)
    out['item/smithing_guide.png'] = sprite(BOOK, {'o':IRON[0], 'h':WOOD[2], 'H':WOOD[3], 'm':WOOD[1], 'b':BRONZE[2], 'B':BRONZE[3], 'p':'#dbc9a0', 'q':'#af946c'})

    # Soot-dark courses. Large colour clusters and edge wear, never stochastic noise.
    im,d=canvas((16,16), IRON[0])
    for y,offset in [(0,0),(4,4),(8,0),(12,4)]:
        for x in range(offset-8,16,8):
            d.rectangle((x,y,x+6,y+2), fill=IRON[1]); d.line((x+1,y,x+5,y), fill=IRON[2])
    for box in [(2,1,4,1),(10,9,12,9),(6,5,8,5),(1,13,2,13)]: d.rectangle(box, fill='#343d45')
    out['block/forge_masonry.png']=im
    im,d=canvas((16,16),IRON[1])
    d.rectangle((0,0,15,1), fill=IRON[3]); d.line((0,2,15,2), fill=IRON[2]); d.line((0,14,15,14), fill=IRON[0])
    for x,y in [(2,5),(11,11),(8,3)]: d.line((x,y,x+2,y),fill=IRON[2])
    out['block/iron_plate.png']=im
    im,d=canvas((16,16),IRON[2])
    for y in [0,8]:
        d.line((0,y,15,y),fill=IRON[4]); d.line((0,y+7,15,y+7),fill=IRON[0])
        for x in [2,13]:
            d.rectangle((x,y+3,x+1,y+4),fill=IRON[0]); d.point((x,y+3),fill=BRONZE[2]); d.point((x+1,y+3),fill=BRONZE[3])
    out['block/iron_strap.png']=im
    im,d=canvas((16,16),'#181a1c')
    for box in [(1,3,5,4),(9,1,12,2),(6,10,12,12),(0,14,3,15)]: d.rectangle(box,fill='#242528')
    d.line((2,4,4,4),fill='#303136'); d.line((8,10,11,10),fill='#343238')
    out['block/forge_inner.png']=im
    im,d=canvas((16,16),'#221c1b')
    for x,y in [(2,2),(9,1),(5,7),(12,10),(1,12)]:
        d.rectangle((x,y,x+2,y+1), fill='#853f2b'); d.point((x+1,y),fill='#d98438')
    out['block/forge_coals.png']=im
    im,d=canvas((16,16),IRON[2])
    d.rectangle((1,1,14,14),fill=IRON[3]); d.line((1,1,14,1),fill=IRON[5]); d.line((1,2,1,12),fill=IRON[4])
    for box in [(4,4,7,4),(8,6,11,6),(3,8,5,8),(7,10,10,10),(10,3,12,3)]:d.rectangle(box,fill=IRON[4])
    d.rectangle((11,11,12,12),fill=IRON[0]); d.line((11,13,13,13),fill=IRON[4])
    out['block/smiths_anvil_top.png']=im
    im,d=canvas((16,16),IRON[2]);d.rectangle((0,0,15,2),fill=IRON[3]);d.line((0,0,15,0),fill=IRON[4]);d.rectangle((0,12,15,15),fill=IRON[1])
    for box in [(2,5,4,5),(10,3,13,3),(7,9,9,9)]: d.rectangle(box,fill=IRON[3])
    out['block/smiths_anvil_body.png']=im
    im,d=canvas((16,16),WOOD[2])
    for x in [0,5,10,15]: d.line((x,0,x,15),fill=WOOD[0]);d.line((x+1,0,x+1,15),fill=WOOD[3])
    for pts in [[(3,1),(3,5),(2,6),(2,9)],[(8,8),(7,9),(7,13)],[(13,2),(12,3),(12,6)],[(13,10),(13,15)]]:d.line(pts,fill=WOOD[1])
    d.line((2,0,4,0),fill=WOOD[4]);d.line((11,0,14,0),fill=WOOD[4])
    out['block/worked_timber.png']=im
    im,d=canvas((16,16),WOOD[2])
    for a,c in [(0,WOOD[0]),(1,WOOD[3]),(3,WOOD[1]),(4,WOOD[4]),(6,WOOD[1])]: d.rectangle((a,a,15-a,15-a),outline=c)
    d.line((8,0,8,3),fill=WOOD[0]); d.line((13,11,15,13),fill=WOOD[0])
    out['block/timber_end.png']=im
    im,d=canvas((16,16),'#555b5a')
    for a,c in [(0,'#343b3d'),(1,'#9a9d8d'),(3,'#737d78'),(5,'#474f51')]:d.rectangle((a,a,15-a,15-a),outline=c)
    d.rectangle((6,6,9,9),fill=IRON[1]); d.rectangle((7,7,8,8),fill=BRONZE[2]);d.point((7,7),fill=BRONZE[3])
    for p in [(3,2),(12,5),(4,12),(10,10)]: d.point(p,fill='#a3a698')
    out['block/grinding_wheel_side.png']=im
    im,d=canvas((16,16),'#656d68')
    for y in [1,5,9,13]:d.line((0,y,15,y),fill='#91988a');d.line((2,y+1,9,y+1),fill='#767f75')
    for x,y in [(3,3),(11,7),(7,15)]:d.line((x,y,x+2,y),fill='#464e4e')
    out['block/grinding_wheel_edge.png']=im
    im,d=canvas((16,16),IRON[2]); d.rectangle((0,14,15,15),fill=IRON[3]); d.line((0,14,15,14),fill=IRON[4]); d.rectangle((4,6,11,9),fill=IRON[4]); d.line((4,6,11,6),fill=IRON[5]); d.line((4,9,11,9),fill=IRON[2]); d.line((7,7,9,7),fill=IRON[3])
    out['block/billet.png']=im
    # Molten metal: dark oxide rafts and bright channels. Four discrete, slow frames.
    im,d=canvas((16,64))
    for f in range(4):
        d.rectangle((0,f*16,15,f*16+15),fill=HEAT[3])
        for x,y,w,h in [(1,1,4,2),(9,3,5,3),(3,8,6,2),(11,12,4,2),(0,14,5,2)]:
            dy=(y+f//2)%16
            d.rectangle((x,f*16+dy,x+w-1,f*16+min(15,dy+h)),fill=HEAT[2]);d.line((x,f*16+dy,x+w-2,f*16+dy),fill=HEAT[1])
        for pts in [[(0,5),(4,5),(5,6),(8,6)],[(8,0),(8,1),(7,2)],[(4,12),(7,12),(8,13),(10,13)]]:
            d.line([(x,f*16+min(15,y+f%2)) for x,y in pts],fill=HEAT[4])
        d.line((1,f*16+6+f%2,3,f*16+6+f%2),fill=HEAT[5])
    out['block/molten_metal.png']=im
    for frame in range(4):
        im,d=canvas((8,8))
        for x,y in [(3,2),(3,3),(3,4),(4,3)][:4-frame]:d.point((x,y),fill='white')
        out[f'particle/spark_{frame}.png']=im
    # Restrained forged frame. Source rectangles are stable resource-pack contracts.
    im,d=canvas((256,256))
    for a,c in [(0,IRON[0]),(1,IRON[4]),(2,IRON[2]),(3,IRON[1]),(4,IRON[0]),(5,'#26292d')]:
        d.rectangle((a,a,63-a,63-a),fill=c)
    for x,y in [(3,3),(59,3),(3,59),(59,59)]:d.rectangle((x,y,x+1,y+1),fill=BRONZE[2]);d.point((x,y),fill=BRONZE[3])
    d.line((8,2,55,2),fill=BRONZE[1])
    d.rectangle((64,0,95,31),fill=IRON[0]);d.line((64,31,95,31),fill=IRON[3]);d.line((95,0,95,31),fill=IRON[3]);d.rectangle((66,2,93,29),fill='#171a1e')
    # Hard edged targets; alpha only at the outermost circle boundary is unnecessary at pixel GUI scale.
    for x,y in [(96,0),(160,0)]:
        d.ellipse((x+2,y+2,x+61,y+61),fill='white')
    d.ellipse((102,6,153,57),fill=(0,0,0,0))
    out['gui/smithing.png']=im
    im,d=canvas((256,256))
    for a,c in [(0,IRON[0]),(1,WOOD[3]),(2,WOOD[1]),(8,WOOD[0]),(9,'#b99c6d'),(10,'#d6c29a'),(12,'#e5d5b4')]:d.rectangle((a,a,63-a,63-a),fill=c)
    for x,y in [(4,4),(58,4),(4,58),(58,58)]:d.point((x,y),fill=BRONZE[3])
    d.rectangle((64,0,95,31),fill='#bba077');d.rectangle((65,1,94,30),fill='#d4be94')
    out['gui/guide.png']=im
    return out
