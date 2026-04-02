import { mkdirSync, readdirSync, rmSync, statSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join } from 'node:path';

const outDir = 'out';
const classesDir = `${outDir}/classes`;
const jacocoExec = `${outDir}/jacoco.exec`;

rmSync(outDir, { recursive: true, force: true });
mkdirSync(classesDir, { recursive: true });

const MINECRAFT_DEPENDENT = [
  'AudiolayerMod.java',
  'AudiolayerClientSetup.java',
  'AudiolayerCommands.java',
  'AudiolayerServerCommands.java',
  'AudiolayerClientHandler.java',
  'AudiolayerPlayPacket.java',
  'AudiolayerStopPacket.java',
  'ClientAudiolayerApi.java',
  'AudiolayerSoundInstance.java',
  'SoundSeekUtil.java',
  'Jave2AudioConversionService.java',
  'ExternalFfmpegAudioConversionService.java',
];
const mainSources = listJava('src/main/java').filter((file) => !MINECRAFT_DEPENDENT.some((name) => file.endsWith(name)));
const testSources = listJava('src/test/java');

if (mainSources.length === 0 && testSources.length === 0) {
  console.log('No Java sources found, skipping tests.');
  process.exit(0);
}

const jacocoAgent = execFileSync(process.platform === 'win32' ? 'gradlew.bat' : './gradlew', ['--quiet', 'printJacocoAgentPath'], { encoding: 'utf8' }).trim();

execFileSync('javac', ['-d', classesDir, ...mainSources, ...testSources], { stdio: 'inherit' });
execFileSync('java', [`-javaagent:${jacocoAgent}=destfile=${jacocoExec},append=false`, '-ea', '-cp', classesDir, 'com.audiolayer.testsupport.TestRunner'], { stdio: 'inherit' });
execFileSync(process.platform === 'win32' ? 'gradlew.bat' : './gradlew', ['coverageVerification'], { stdio: 'inherit' });

function listJava(root) {
  const result = [];
  function walk(dir) {
    for (const entry of readdirSync(dir)) {
      const full = join(dir, entry);
      const stat = statSync(full);
      if (stat.isDirectory()) walk(full);
      else if (full.endsWith('.java')) result.push(full);
    }
  }
  try { walk(root); } catch { /* directory does not exist */ }
  return result;
}
