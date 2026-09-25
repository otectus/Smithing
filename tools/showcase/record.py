#!/usr/bin/env python3
"""Run inside a dedicated Gamescope display; record only that display and a private game audio sink."""
import argparse,json,os,signal,subprocess,time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--take',required=True)
    parser.add_argument('--width',type=int,default=1280)
    parser.add_argument('--height',type=int,default=720)
    args=parser.parse_args()
    out=ROOT/'build/showcase'/args.take
    out.mkdir(parents=True,exist_ok=False)
    sink=f'smithing_showcase_{os.getpid()}'
    module=subprocess.check_output(['pactl','load-module','module-null-sink',f'sink_name={sink}','sink_properties=device.description=SmithingShowcase'],text=True).strip()
    game=None;record=None
    env=os.environ.copy()
    env['PULSE_SINK']=sink
    env['ALSOFT_DRIVERS']='pulse'
    env['JAVA_TOOL_OPTIONS']=f'-Dimmersive_smithing.showcase.output={out}'
    log=open(out/'client.log','w')
    capture_log=open(out/'capture.log','w')
    try:
        game=subprocess.Popen(['/home/otectus/Projects/.mcmod-tools/gradlew-quiet.sh',str(ROOT),'runClient','-PsmokeTest','--offline'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
        deadline=time.monotonic()+150
        while not (out/'ready').exists():
            if game.poll() is not None:raise RuntimeError(f'Client exited before recording: {game.returncode}')
            if time.monotonic()>deadline:raise RuntimeError('Client did not prepare showcase')
            time.sleep(.1)
        # Video comes directly from the game framebuffer; only the private game sink is recorded here.
        record=subprocess.Popen(['ffmpeg','-hide_banner','-loglevel','warning','-y',
            '-thread_queue_size','1024','-f','pulse','-i',sink+'.monitor',
            '-c:a','pcm_s16le',str(out/'audio.wav')],stdout=capture_log,stderr=subprocess.STDOUT)
        (out/'recording-start-ms').write_text(str(round(time.time()*1000)))
        code=game.wait(timeout=180)
        if code:raise RuntimeError(f'Client failed: {code}; see {out}/client.log')
        if not (out/'finished').exists():raise RuntimeError('Showcase did not finish successfully')
        record.send_signal(signal.SIGINT)
        record.wait(timeout=20)
        audio_start=int((out/'recording-start-ms').read_text())
        video_start=int((out/'video-start-ms').read_text())
        offset=max(0,(video_start-audio_start)/1000)
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(out/'video.mp4'),
            '-ss',str(offset),'-i',str(out/'audio.wav'),'-c:v','copy','-c:a','aac','-b:a','160k',
            '-shortest','-movflags','+faststart',str(out/'raw.mp4')],check=True)
        (out/'recording-start-ms').write_text(str(video_start))
        print(out,flush=True)
    finally:
        if record is not None and record.poll() is None:
            record.send_signal(signal.SIGINT)
            record.wait(timeout=20)
        if game is not None and game.poll() is None:
            game.terminate()
            game.wait(timeout=20)
        subprocess.run(['pactl','unload-module',module],check=False)
        log.close();capture_log.close()

if __name__=='__main__':main()
