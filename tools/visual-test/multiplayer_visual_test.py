#!/usr/bin/env python3
"""Run two real Forge clients against a disposable local server and verify remote tool swings."""
import argparse
import hashlib
import json
import os
import pathlib
import re
import shutil
import socket
import subprocess
import time
import uuid

ROOT = pathlib.Path(__file__).resolve().parents[2]
INSTALL = pathlib.Path('/home/otectus/Documents/curseforge/minecraft/Install')
FORGE_VERSION = 'forge-47.4.23'
DEFAULT_XVFB = pathlib.Path('/tmp/jewelcraft-xvfb/usr/bin/Xvfb')


def allowed(rules):
    answer = not rules
    for rule in rules or []:
        os_rule = rule.get('os', {})
        matches = (os_rule.get('name', 'linux') == 'linux'
                   and os_rule.get('arch', 'amd64') in ('amd64', 'x86_64')
                   and not rule.get('features'))
        if matches:
            answer = rule['action'] == 'allow'
    return answer


def launcher_data():
    vanilla = json.loads((INSTALL / 'versions/1.20.1/1.20.1.json').read_text())
    forge = json.loads((INSTALL / f'versions/{FORGE_VERSION}/{FORGE_VERSION}.json').read_text())
    libraries = {}
    for library in vanilla['libraries'] + forge['libraries']:
        if allowed(library.get('rules')):
            parts = library['name'].split(':')
            libraries[':'.join(parts[:2]) + (':' + parts[3] if len(parts) > 3 else '')] = library
    classpath = [str(INSTALL / 'libraries' / library['downloads']['artifact']['path'])
                 for library in libraries.values() if library.get('downloads', {}).get('artifact')]
    classpath.append(str(INSTALL / 'versions/1.20.1/1.20.1.jar'))
    return vanilla, forge, ':'.join(classpath)


def expand(arguments, values):
    result = []
    for argument in arguments:
        if isinstance(argument, dict):
            if not allowed(argument.get('rules')):
                continue
            argument = argument['value']
        for value in argument if isinstance(argument, list) else [argument]:
            value = re.sub(r'\$\{([^}]+)\}', lambda match: values[match.group(1)], value)
            if value.startswith('-DignoreList='):
                value += ',1.20.1.jar'
            result.append(value)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-dir', type=pathlib.Path,
                        default=ROOT / 'build' / 'visual-test' / time.strftime('%Y%m%d-%H%M%S'))
    parser.add_argument('--xvfb', type=pathlib.Path,
                        help='optional Xvfb binary; omit to use the current graphical session')
    parser.add_argument('--display', default=':96')
    parser.add_argument('--timeout', type=int, default=480)
    parser.add_argument('--fullscreen-clients', action='store_true',
                        help='use Minecraft fullscreen for landscape captures under tiling window managers')
    parser.add_argument('--server-libraries', type=pathlib.Path, default=INSTALL / 'libraries',
                        help='libraries directory from a Forge 47.4.23 dedicated-server installation')
    args = parser.parse_args()
    work = args.work_dir.resolve()
    if work.exists():
        raise SystemExit(f'Refusing to reuse {work}')
    if args.xvfb is not None and not args.xvfb.is_file():
        raise SystemExit(f'Headless X server not found: {args.xvfb}')
    server_libraries = args.server_libraries.resolve()
    server_args = server_libraries / 'net/minecraftforge/forge/1.20.1-47.4.23/unix_args.txt'
    if not server_args.is_file():
        raise SystemExit('A client installation does not contain dedicated-server libraries. '
                         'Pass --server-libraries from a Forge 47.4.23 server installation.')
    libs = ROOT / 'build' / 'libs'
    mod = max((path for path in libs.glob('immersive-smithing-*.jar') if not path.stem.endswith('-packtest')),
              key=lambda path: path.stat().st_mtime)
    agent = max(libs.glob('immersive-smithing-*-packtest.jar'), key=lambda path: path.stat().st_mtime)
    vanilla, forge, classpath = launcher_data()
    work.mkdir(parents=True)
    shared = work / 'shared'
    shared.mkdir()
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]

    def prepare(folder):
        folder.mkdir()
        (folder / 'mods').mkdir()
        for artifact in (mod, agent):
            shutil.copy2(artifact, folder / 'mods' / artifact.name)

    server = work / 'server'
    prepare(server)
    (server / 'libraries').symlink_to(server_libraries, target_is_directory=True)
    (server / 'natives').mkdir()
    (server / 'eula.txt').write_text('eula=true\n')
    (server / 'server.properties').write_text(
        'online-mode=false\nenforce-secure-profile=false\nserver-ip=127.0.0.1\n'
        f'server-port={port}\nlevel-type=minecraft:flat\ngenerate-structures=false\n'
        'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},'
        '{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],'
        '"lakes":false,"features":false,"structure_overrides":[]}\n'
        'difficulty=peaceful\nspawn-monsters=false\nview-distance=3\nsimulation-distance=3\nspawn-protection=0\n')
    common_values = {'classpath': classpath, 'classpath_separator': ':',
                     'library_directory': str(INSTALL / 'libraries'),
                     'natives_directory': str(server / 'natives'),
                     'launcher_name': 'ImmersiveSmithingVisualTest', 'launcher_version': '1',
                     'version_name': FORGE_VERSION}
    properties = [f'-Dimmersive_smithing.packtest.multiplayer=true',
                  f'-Dimmersive_smithing.packtest.shared={shared}']
    server_command = ['java', '-Xmx2G', '-Xms256M', *properties, f'@{server_args}', 'nogui']
    (server / 'launch-command.json').write_text(json.dumps(server_command, indent=2) + '\n')

    processes = []
    logs = []
    clients = []
    environment = os.environ.copy()
    if args.xvfb is not None:
        environment.update(DISPLAY=args.display, LIBGL_ALWAYS_SOFTWARE='1')

    def spawn(command, cwd, log_path, env=environment):
        output = log_path.open('w')
        logs.append(output)
        process = subprocess.Popen(command, cwd=cwd, stdin=subprocess.PIPE, stdout=output,
                                   stderr=subprocess.STDOUT, text=True, env=env)
        processes.append(process)
        return process

    def wait_until(predicate, timeout):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if predicate():
                return
            failed = [process for process in processes if process.poll() not in (None, 0)]
            if failed:
                raise RuntimeError(f'Runtime exited with an error; inspect {work}')
            time.sleep(.25)
        raise RuntimeError(f'Timed out; inspect {work}')

    try:
        if args.xvfb is not None:
            spawn([str(args.xvfb), args.display, '-screen', '0', '2200x900x24',
                   '-nolisten', 'tcp', '-ac'], work, work / 'xvfb.log')
            display_number = args.display.removeprefix(':').split('.')[0]
            wait_until(lambda: pathlib.Path('/tmp/.X11-unix', 'X' + display_number).exists(), 30)
        dedicated = spawn(server_command, server, server / 'console.log')
        wait_until(lambda: 'Done (' in (server / 'console.log').read_text(errors='replace'), 180)

        for role, name in [('leader', 'VisualLeader'), ('peer', 'VisualPeer')]:
            folder = work / role
            prepare(folder)
            (folder / 'natives').mkdir()
            (folder / 'options.txt').write_text(
                f'fullscreen:{str(args.fullscreen_clients).lower()}\n'
                'onboardAccessibility:false\npauseOnLostFocus:false\nguiScale:2\nrenderDistance:3\n'
                'tutorialStep:none\ngamma:1.0\n'
                'soundCategory_master:0.0\n')
            values = dict(common_values,
                          auth_player_name=name, version_name=FORGE_VERSION, game_directory=str(folder),
                          assets_root=str(INSTALL / 'assets'), assets_index_name=vanilla['assetIndex']['id'],
                          auth_uuid=uuid.uuid3(uuid.NAMESPACE_DNS, 'OfflinePlayer:' + name).hex,
                          auth_access_token='0', clientid='', auth_xuid='', user_type='legacy',
                          version_type='release', natives_directory=str(folder / 'natives'),
                          launcher_name='ImmersiveSmithingVisualTest', launcher_version='1')
            props = [f'-Dimmersive_smithing.packtest.output={folder}',
                     '-Dimmersive_smithing.packtest.multiplayer=true',
                     f'-Dimmersive_smithing.packtest.role={role}',
                     f'-Dimmersive_smithing.packtest.server=127.0.0.1:{port}',
                     f'-Dimmersive_smithing.packtest.shared={shared}']
            command = (['java', '-Xmx2G', '-Xms256M', *props,
                        *expand(vanilla['arguments']['jvm'] + forge['arguments']['jvm'], values), forge['mainClass'],
                        *expand(vanilla['arguments']['game'] + forge['arguments']['game'], values),
                        '--width', '1280', '--height', '720'])
            (folder / 'launch-command.json').write_text(json.dumps(command, indent=2) + '\n')
            clients.append(spawn(command, folder, folder / 'console.log'))

        wait_until(lambda: all((work / role / 'packtest-result.json').is_file()
                               for role in ('leader', 'peer')), args.timeout)
        results = {role: json.loads((work / role / 'packtest-result.json').read_text())
                   for role in ('leader', 'peer')}
        for process in clients:
            if process.wait(timeout=60) != 0:
                raise RuntimeError('Client exited with an error')
        dedicated.stdin.write('stop\n')
        dedicated.stdin.flush()
        if dedicated.wait(timeout=60) != 0:
            raise RuntimeError('Dedicated server exited with an error')
        screenshots = sorted(str(path.relative_to(work)) for path in work.glob('*/screenshots/packtest_multiplayer_*.png'))
        passed = all(result.get('pass') for result in results.values()) and len(screenshots) >= 4
        summary = {'pass': passed, 'results': results, 'screenshots': screenshots,
                   'artifacts': {artifact.name: hashlib.sha256(artifact.read_bytes()).hexdigest()
                                 for artifact in (mod, agent)}}
        (work / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
        if not passed:
            raise RuntimeError(f'Multiplayer visual assertions failed; inspect {work / "summary.json"}')
        print('PASS: remote loaded-tongs and hammer swings synchronized across two real clients')
        print(f'Evidence: {work}')
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=10)
        for output in logs:
            output.close()


if __name__ == '__main__':
    main()
