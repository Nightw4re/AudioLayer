import { mkdirSync, readdirSync, readFileSync, rmSync, statSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { createWriteStream } from 'node:fs';
import { pipeline } from 'node:stream/promises';
import { join } from 'node:path';

const outDir = 'out';
const classesDir = `${outDir}/classes`;
const jacocoExec = `${outDir}/jacoco.exec`;
const toolsDir = `${outDir}/tools`;
const reportDir = 'build/reports/jacoco/html';
const jacocoVersion = '0.8.12';
const jacocoAgentJar = `${toolsDir}/jacocoagent.jar`;
const jacocoCliJar = `${toolsDir}/jacococli.jar`;
const mode = process.argv[2] ?? 'test';

rmSync(outDir, { recursive: true, force: true });
mkdirSync(classesDir, { recursive: true });
mkdirSync(toolsDir, { recursive: true });

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
  'Mp3SoundInstance.java',
  'Mp3StreamDecoder.java',
  'Jave2AudioConversionService.java',
  'OggVorbisEncoder.java',
  'ExternalFfmpegAudioConversionService.java',
];
const mainSources = listJava('src/main/java').filter((file) => !MINECRAFT_DEPENDENT.some((name) => file.endsWith(name)));
const testSources = listJava('src/test/java');

if (mainSources.length === 0 && testSources.length === 0) {
  console.log('No Java sources found, skipping tests.');
  process.exit(0);
}

await ensureDownloaded(
  `https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.agent/${jacocoVersion}/org.jacoco.agent-${jacocoVersion}-runtime.jar`,
  jacocoAgentJar,
);
await ensureDownloaded(
  `https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.cli/${jacocoVersion}/org.jacoco.cli-${jacocoVersion}-nodeps.jar`,
  jacocoCliJar,
);

execFileSync('javac', ['-d', classesDir, ...mainSources, ...testSources], { stdio: 'inherit' });
if (mode === 'test') {
  execFileSync('java', ['-ea', '-cp', classesDir, 'com.audiolayer.testsupport.TestRunner'], { stdio: 'inherit' });
} else if (mode === 'coverage') {
  execFileSync('java', [`-javaagent:${jacocoAgentJar}=destfile=${jacocoExec},append=false`, '-ea', '-cp', classesDir, 'com.audiolayer.testsupport.TestRunner'], { stdio: 'inherit' });
  mkdirSync(reportDir, { recursive: true });
  execFileSync('java', ['-jar', jacocoCliJar, 'report', jacocoExec, '--classfiles', classesDir, '--sourcefiles', 'src/main/java', '--html', reportDir, '--xml', `${outDir}/jacoco.xml`], { stdio: 'inherit' });
  verifyCoverage(`${outDir}/jacoco.xml`, 0.80);
} else {
  throw new Error(`Unknown mode: ${mode}`);
}

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

async function ensureDownloaded(url, destination) {
  try {
    statSync(destination);
    return;
  } catch {
    // download below
  }

  const response = await fetch(url);
  if (!response.ok || !response.body) {
    throw new Error(`Failed to download ${url}: ${response.status} ${response.statusText}`);
  }

  const fileStream = createWriteStream(destination);
  await pipeline(response.body, fileStream);
}

function verifyCoverage(xmlPath, minimumRatio) {
  const xml = readFileSync(xmlPath, 'utf8');
  const match = xml.match(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/);
  if (!match) {
    throw new Error('Could not determine line coverage from JaCoCo report.');
  }

  const missed = Number(match[1]);
  const covered = Number(match[2]);
  const ratio = covered / (covered + missed);
  if (ratio < minimumRatio) {
    throw new Error(`Line coverage ${Math.round(ratio * 1000) / 10}% is below required ${(minimumRatio * 100)}%.`);
  }
}
