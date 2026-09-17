import { Link } from 'react-router-dom'
import {
  Boxes,
  Sparkles,
  Users,
  FileCode2,
  FileDown,
  Zap,
  ArrowRight,
  Check,
  Bug,
} from 'lucide-react'

function Logo({ className = 'w-9 h-9' }: { className?: string }) {
  return (
    <div className={`${className} rounded-lg bg-blue-600 flex items-center justify-center shrink-0`}>
      <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    </div>
  )
}

const FEATURES = [
  {
    icon: Boxes,
    title: 'Modelado UML de clases',
    description: 'Clases, interfaces, enumeraciones, atributos, métodos y relaciones con multiplicidades sobre un lienzo fluido.',
  },
  {
    icon: Users,
    title: 'Colaboración en tiempo real',
    description: 'Varios usuarios editan el mismo diagrama a la vez, con presencia, cursores remotos y sincronización por WebSocket.',
  },
  {
    icon: Sparkles,
    title: 'Asistente con IA',
    description: 'Crea y edita el diagrama conversando, por dictado de voz o a partir de una imagen del diagrama.',
  },
  {
    icon: FileDown,
    title: 'Exportación XMI 2.5.1',
    description: 'Descarga el modelo en XMI compatible con Enterprise Architect y otras herramientas UML.',
  },
  {
    icon: FileCode2,
    title: 'Genera el backend',
    description: 'Obtén un proyecto Spring Boot con las 4 capas y DTOs a partir del diagrama, listo para compilar.',
  },
  {
    icon: Zap,
    title: 'Rápido y con atajos',
    description: 'Herramientas de selección, mano y atajos de teclado para modelar sin soltar el teclado.',
  },
]

const STEPS = [
  { title: 'Crea tu proyecto', description: 'Organiza tus diagramas dentro de proyectos y comparte el trabajo con tu equipo.' },
  { title: 'Modela el diagrama', description: 'Arrastra clases y relaciones, o pídeselo al asistente de IA y ajústalo en el lienzo.' },
  { title: 'Exporta o genera', description: 'Descarga el XMI 2.5.1 o genera un backend Spring Boot listo para ejecutar.' },
]

export function LandingPage() {
  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <header className="sticky top-0 z-40 border-b border-gray-800/80 bg-gray-950/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
          <a href="#top" className="flex items-center gap-2.5 rounded-lg">
            <Logo />
            <span className="text-base font-bold tracking-tight">UMLPilot</span>
          </a>

          <nav aria-label="Navegación principal" className="hidden items-center gap-6 text-sm text-gray-400 md:flex">
            <a href="#features" className="transition-colors hover:text-gray-100">Características</a>
            <a href="#how" className="transition-colors hover:text-gray-100">Cómo funciona</a>
          </nav>

          <div className="flex items-center gap-2">
            <Link
              to="/login"
              className="rounded-lg px-3 py-2 text-sm font-medium text-gray-300 transition-colors hover:bg-gray-800 hover:text-gray-100"
            >
              Iniciar sesión
            </Link>
            <Link
              to="/register"
              className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-500"
            >
              Crear cuenta
            </Link>
          </div>
        </div>
      </header>

      <main id="top">
        {/* Hero */}
        <section className="relative overflow-hidden" aria-labelledby="hero-title">
          <div className="pointer-events-none absolute inset-0 bg-grid opacity-60" aria-hidden="true" />
          <div className="pointer-events-none absolute left-1/2 top-[-12rem] h-96 w-[42rem] -translate-x-1/2 rounded-full bg-blue-600/10 blur-3xl" aria-hidden="true" />

          <div className="relative mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
            <div>
              <span className="inline-flex items-center gap-2 rounded-full border border-gray-700 bg-gray-900/70 px-3 py-1 text-xs font-medium text-blue-300">
                <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
                Modelado UML con IA y colaboración
              </span>

              <h1 id="hero-title" className="mt-5 text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
                Diseña tus diagramas UML
                <span className="block text-blue-400">y genera código en segundos</span>
              </h1>

              <p className="mt-5 max-w-xl text-base leading-relaxed text-gray-400 sm:text-lg">
                UMLPilot es un editor de diagramas de clases colaborativo: modela en equipo en tiempo real,
                deja que la IA te ayude y exporta XMI 2.5.1 o un backend Spring Boot listo para compilar.
              </p>

              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <Link
                  to="/register"
                  className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-blue-500"
                >
                  Empezar gratis
                  <ArrowRight className="h-4 w-4" aria-hidden="true" />
                </Link>
                <Link
                  to="/login"
                  className="flex items-center justify-center gap-2 rounded-lg border border-gray-700 bg-gray-900 px-5 py-3 text-sm font-semibold text-gray-200 transition-colors hover:bg-gray-800"
                >
                  Ya tengo cuenta
                </Link>
              </div>

              <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-400">
                {['Sin instalar nada', 'Colaboración en vivo', 'Exporta y genera código'].map((item) => (
                  <li key={item} className="flex items-center gap-2">
                    <Check className="h-4 w-4 text-green-400" aria-hidden="true" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>

            {/* Product preview */}
            <div className="relative" aria-hidden="true">
              <div className="rounded-2xl border border-gray-800 bg-gray-900/80 shadow-2xl shadow-black/50">
                <div className="flex items-center gap-1.5 border-b border-gray-800 px-4 py-3">
                  <span className="h-2.5 w-2.5 rounded-full bg-red-500/80" />
                  <span className="h-2.5 w-2.5 rounded-full bg-yellow-500/80" />
                  <span className="h-2.5 w-2.5 rounded-full bg-green-500/80" />
                  <span className="ml-3 text-xs text-gray-500">diagrama — umlpilot</span>
                </div>

                <div className="relative h-72 bg-grid p-5">
                  <div className="absolute left-1/2 top-3 flex -translate-x-1/2 items-center gap-1.5 rounded-full border border-gray-700 bg-gray-950/90 px-3 py-1 text-[11px] text-gray-300">
                    <Users className="h-3 w-3 text-green-400" />
                    2 en línea
                  </div>

                  <div className="absolute left-6 top-16 w-40 rounded-lg border border-gray-700 bg-gray-800/95 p-3">
                    <p className="mb-2 text-xs font-semibold text-gray-100">Cliente</p>
                    <div className="space-y-1">
                      <div className="h-1.5 w-24 rounded bg-gray-600" />
                      <div className="h-1.5 w-16 rounded bg-gray-600" />
                    </div>
                  </div>

                  <div className="absolute right-6 top-40 w-40 rounded-lg border border-gray-700 bg-gray-800/95 p-3">
                    <p className="mb-2 text-xs font-semibold text-gray-100">Pedido</p>
                    <div className="space-y-1">
                      <div className="h-1.5 w-20 rounded bg-gray-600" />
                      <div className="h-1.5 w-24 rounded bg-gray-600" />
                    </div>
                  </div>

                  <svg className="absolute inset-0 h-full w-full" viewBox="0 0 400 288" preserveAspectRatio="none">
                    <path d="M170 96 C 230 96, 220 176, 270 176" fill="none" stroke="#9ca3af" strokeWidth="2" />
                  </svg>

                  <div className="absolute bottom-4 right-4 rounded-lg border border-blue-800 bg-blue-950/80 px-3 py-2 text-[11px] text-blue-200">
                    <Sparkles className="mr-1 inline h-3 w-3" />
                    La IA añadió 3 clases
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features */}
        <section id="features" className="border-t border-gray-800/80 py-16 sm:py-20" aria-labelledby="features-title">
          <div className="mx-auto max-w-6xl px-4 sm:px-6">
            <div className="max-w-2xl">
              <h2 id="features-title" className="text-2xl font-bold sm:text-3xl">Todo lo que necesitas para modelar</h2>
              <p className="mt-3 text-gray-400">
                Un editor pensado para trabajar en equipo, apoyarte en la IA y llevar tu modelo hasta el código.
              </p>
            </div>

            <ul className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map(({ icon: Icon, title, description }) => (
                <li key={title} className="rounded-xl border border-gray-800 bg-gray-900/60 p-5 transition-colors hover:border-gray-700">
                  <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-blue-950">
                    <Icon className="h-5 w-5 text-blue-400" aria-hidden="true" />
                  </div>
                  <h3 className="text-base font-semibold text-gray-100">{title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-400">{description}</p>
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* How it works */}
        <section id="how" className="border-t border-gray-800/80 py-16 sm:py-20" aria-labelledby="how-title">
          <div className="mx-auto max-w-6xl px-4 sm:px-6">
            <h2 id="how-title" className="text-2xl font-bold sm:text-3xl">Cómo funciona</h2>
            <ol className="mt-10 grid gap-6 sm:grid-cols-3">
              {STEPS.map((step, index) => (
                <li key={step.title} className="rounded-xl border border-gray-800 bg-gray-900/60 p-5">
                  <span className="mb-4 flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">
                    {index + 1}
                  </span>
                  <h3 className="text-base font-semibold text-gray-100">{step.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-400">{step.description}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* CTA */}
        <section className="border-t border-gray-800/80 py-16 sm:py-20" aria-labelledby="cta-title">
          <div className="mx-auto max-w-4xl px-4 text-center sm:px-6">
            <h2 id="cta-title" className="text-2xl font-bold sm:text-3xl">Empieza a modelar hoy</h2>
            <p className="mx-auto mt-3 max-w-xl text-gray-400">
              Crea tu cuenta gratis y lleva tus diagramas de clases del boceto al código.
            </p>
            <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
              <Link
                to="/register"
                className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-blue-500"
              >
                Crear cuenta gratis
                <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </Link>
              <Link
                to="/login"
                className="flex items-center justify-center rounded-lg border border-gray-700 bg-gray-900 px-6 py-3 text-sm font-semibold text-gray-200 transition-colors hover:bg-gray-800"
              >
                Iniciar sesión
              </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-gray-800/80 py-8">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 sm:flex-row sm:px-6">
          <div className="flex items-center gap-2.5">
            <Logo className="w-7 h-7" />
            <span className="text-sm font-semibold">UMLPilot</span>
          </div>
          <p className="text-xs text-gray-500">Editor UML colaborativo con IA · {new Date().getFullYear()}</p>
          <a
            href="https://github.com/anomalyco/opencode/issues"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-xs text-gray-500 transition-colors hover:text-gray-300"
          >
            <Bug className="h-3.5 w-3.5" aria-hidden="true" />
            Reportar un problema
          </a>
        </div>
      </footer>
    </div>
  )
}
