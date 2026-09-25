#!/usr/bin/env python3
"""Edit marked, real gameplay into a short captioned MP4 without covering the game interface."""
import argparse,concurrent.futures,json,math,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
FONT='/usr/share/fonts/noto/NotoSans-Regular.ttf'
TITLE='/usr/share/fonts/noto/NotoSerif-Bold.ttf'
def main():
    p=argparse.ArgumentParser();p.add_argument('--take',required=True);a=p.parse_args()
    take=ROOT/'build/showcase'/a.take
    result=json.loads((take/'result.json').read_text());assert result['pass']
    start=int((take/'recording-start-ms').read_text())
    m={x['name']:(x['wall_ms']-start)/1000 for x in result['markers']}
    scenes=[]
    def add(name,begin,duration,caption,card=False):
        scenes.append(dict(name=name,start=max(0,begin),duration=round(duration*30)/30,caption=caption,card=card))
    add('intro',m['workshop']+.7,3.5,'A hands-on smithing mod for Minecraft',True)
    add('heat',m['heat']+.5,min(4,m['choose']-m['heat']-.7),'MELT  /  Fuel the forge. Bring your metal to life.')
    add('choose',m['choose']+.4,2,'CHOOSE  /  Turn molten metal into equipment.')
    add('forge',m['forge_play']+.1,min(6.3,m['forge_result']-m['forge_play']-.15),'FORGE  /  Time each pass to improve durability.')
    add('forge_result',m['forge_result']+.15,1.4,'YOUR TIMING MATTERS  /  Quality starts at the forge.')
    add('transfer',m['transfer']+.2,2.2,'TRANSFER  /  Carry the hot workpiece with your tongs.')
    add('anvil',m['anvil_play']+.1,4.2,'SHAPE  /  Aim for the centre. Strike as the rings meet.')
    add('anvil_finish',m['anvil_result']-3.8,4.7,'EVERY STRIKE COUNTS  /  Shaping sets efficacy.')
    add('quench',m['quench']+.1,3.2,'QUENCH  /  Cool the shaped metal to finish your item.')
    add('refine',m['refine']+.1,2.9,'REFINE  /  Spend experience to improve your equipment.')
    add('quality',m['quality']+.2,2.8,'MADE BY YOU  /  Your work determines the finished quality.')
    add('guide',m['guide']+.2,3.6,'LEARN THE TRADE  /  A readable, searchable Smithing Guide.')
    add('outro',m['outro']+.5,3.2,'Minecraft Java Edition  /  Forge 1.20.1',True)
    out=ROOT/'output/sneak-peek';out.mkdir(parents=True,exist_ok=True)
    work=take/'edit';work.mkdir(exist_ok=True)
    def render(args):
        i,s=args
        caption=work/f'{i:02d}.txt';caption.write_text(s['caption'])
        vf=['scale=1184:666:flags=lanczos','pad=1280:720:48:0:color=0x131719',
            'drawbox=x=48:y=665:w=1184:h=2:color=0xba8d56:t=fill',
            f'drawtext=fontfile={FONT}:textfile={caption}:fontcolor=0xf1e5cf:fontsize=23:x=64:y=683']
        if s['card']:
            name=work/f'{i:02d}-title.txt';name.write_text('IMMERSIVE SMITHING')
            subtitle=work/f'{i:02d}-subtitle.txt';subtitle.write_text('Gameplay sneak peek' if i==0 else 'Every strike leaves its mark.')
            vf+=['drawbox=x=48:y=0:w=1184:h=666:color=black@0.34:t=fill',
                f'drawtext=fontfile={TITLE}:textfile={name}:fontcolor=0xffe8bb:fontsize=52:x=(w-tw)/2:y=244:shadowcolor=black@0.6:shadowx=2:shadowy=2',
                'drawbox=x=500:y=329:w=280:h=2:color=0xba8d56:t=fill',
                f'drawtext=fontfile={FONT}:textfile={subtitle}:fontcolor=white:fontsize=29:x=(w-tw)/2:y=352']
        if i==0:vf+=['fade=t=in:st=0:d=0.4']
        if i==len(scenes)-1:vf+=[f"fade=t=out:st={s['duration']-.5}:d=0.5"]
        target=work/f'{i:02d}.mp4'
        cmd=['ffmpeg','-hide_banner','-loglevel','error','-y','-ss',str(s['start']),'-t',str(s['duration']),'-i',str(take/'raw.mp4'),
             '-vf',','.join(vf),'-af',f"volume=1.4,alimiter=limit=0.94,afade=t=in:st=0:d=0.035,afade=t=out:st={s['duration']-.05}:d=0.05",
             '-c:v','libx264','-preset','medium','-crf','18','-pix_fmt','yuv420p','-r','30','-threads','3','-c:a','aac','-ar','48000','-ac','2','-b:a','160k',str(target)]
        subprocess.run(cmd,check=True)
        return target
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:clips=list(pool.map(render,enumerate(scenes)))
    listing=work/'concat.txt';listing.write_text(''.join(f"file '{x}'\n" for x in clips))
    final=out/'immersive-smithing-sneak-peek.mp4'
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','concat','-safe','0','-i',str(listing),'-c','copy','-movflags','+faststart',str(final)],check=True)
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-ss','1.5','-i',str(final),'-frames:v','1',str(out/'preview.png')],check=True)
    (out/'edit.json').write_text(json.dumps({'footage':'Actual Minecraft client; scripted demonstration using server-scored minigames and real station interactions.','take':a.take,'scenes':scenes,'total_seconds':sum(s['duration'] for s in scenes)},indent=2)+'\n')
    print(final)
    print('Duration:',round(sum(s['duration'] for s in scenes),2))
if __name__=='__main__':main()
