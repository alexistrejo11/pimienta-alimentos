/** Landing copy and asset paths (from company-data.md). */

export interface LandingNavItem {
  id: string;
  label: string;
}

export interface LandingPillar {
  title: string;
  icon: string;
}

export interface LandingClientLogo {
  name: string;
  src: string;
}

export const LANDING_NAV: LandingNavItem[] = [
  { id: 'empresa', label: 'Empresa' },
  { id: 'servicios', label: 'Servicios' },
  { id: 'higiene', label: 'Higiene' },
  { id: 'clientes', label: 'Clientes' },
];

export const LANDING_CONTENT = {
  tagline: 'Un toque de placer a tus sentidos',
  missionSummary:
    'Desarrollar nuestros productos y servicios de manera continua para satisfacer las necesidades de alimentación de nuestros clientes en higiene, presentación, sabor, valor alimenticio, cantidad y atención.',
  coverImage: '/landing/mision.png',
  sinceYear: '2010',

  empresa: {
    headline: 'La satisfacción más grande es servir…',
    paragraphs: [
      'Surgimos en el año 2010 con la misión de brindar al mercado una propuesta de servicio de alimentos institucionales con el más alto sentido de calidez humana.',
      'Somos una empresa mexicana comprometida con la calidad en el servicio de alimentos, especializada en la administración de cafeterías y comedores institucionales.',
      'Nuestra labor se centra en emplear el mejor talento de nuestros colaboradores para brindar a nuestros clientes una experiencia personalizada.',
      'Contamos con personal altamente calificado con más de 20 años de experiencia en la industria de alimentos, y con muchas ganas de sobresalir, lo que en conjunto se traduce en ofrecer una experiencia que supere las expectativas de nuestros clientes.',
    ],
    image: '/landing/objetivo-1.png',
    imageAlt: 'Platillo preparado con presentación profesional',
  },

  compromiso: {
    intro:
      'Generar en nuestros clientes la mayor satisfacción a través de un servicio personalizado y de la más alta calidad en alimentos, siempre brindando un trato de calidez.',
    badge: 'Attendiendo profesionalmente desde 2010',
    pillars: [
      { title: 'Calidad de materia prima', icon: 'eco' },
      { title: 'Personal altamente calificado', icon: 'groups' },
      { title: 'Buena imagen del personal', icon: 'badge' },
      { title: 'Excelente atención personal', icon: 'support_agent' },
      { title: 'Magnífica presentación de los platillos', icon: 'restaurant' },
      { title: 'Higiene y pulcritud', icon: 'clean_hands' },
    ] satisfies LandingPillar[],
    images: [
      { src: '/landing/nuestro-compromiso-1.png', alt: 'Servicio de alimentos institucionales' },
      { src: '/landing/nuestro-compromiso-2.png', alt: 'Presentación de platillos' },
    ],
  },

  servicios: {
    intro:
      'Atendemos miles de comensales diariamente, asegurándonos que cada detalle y cada platillo esté elaborado con los mejores ingredientes, la mayor higiene y la mejor sazón.',
    paragraphs: [
      'Contamos con personal altamente calificado con más de 20 años de experiencia en la industria de alimentos, logrando así superar las expectativas de nuestros clientes y comensales.',
      'Nuestra infraestructura empresarial nos permite atender a cualquier cliente dentro de la República Mexicana, siempre brindando un soporte y atención personalizada las 24 horas los 365 días del año.',
    ],
    sectors: [
      'Cafeterías escolares',
      'Hospitales',
      'Comedores empresariales',
      'Instituciones',
      'Aeropuertos y terminales de autobuses',
    ],
    offers: [
      'Confianza',
      'Capacitación',
      'Seguridad en el servicio',
      'Experiencia de nuestro personal de más de 10 años',
      'Apoyo las 24 horas los 365 días del año',
      'Capacidad de respuesta rápida a las necesidades del cliente',
      'Conocimiento de la industria',
      'Capacidad financiera',
      'Infraestructura e innovación de vanguardia en el mercado de la alimentación',
      'Conocimiento y aplicación de las normas de higiene',
    ],
    images: [
      { src: '/landing/nuestros-servicios-1.png', alt: 'Operación de servicio de alimentos' },
      { src: '/landing/nuestros-servicios-2.png', alt: 'Comedor institucional' },
    ],
  },

  higiene: {
    intro:
      'Incluimos semestralmente a nuestro personal en un Programa de Análisis Clínicos, aplicable a los colaboradores que manipulan alimentos.',
    clinicalTests: [
      'Coproparasitoscópico',
      'Exudado faríngeo',
      'Frotis de mano',
      'Reacciones febriles, primordialmente',
    ],
    detail:
      'Llevamos a cabo el Programa de Análisis Microbiológico a los alimentos y superficies inertes, con la frecuencia de cada cuatro meses en laboratorios acreditados ante la EMA. Supervisamos que nuestro personal cumpla con las normas de seguridad e higiene. Además garantizamos que en un plazo de 4 a 6 meses se obtendrá el distintivo "H" dependiendo de las instalaciones y con apoyo del cliente. Se cuenta con manual operativo de todos los procesos para garantizar la calidad y con Consultor de Distintivo "H" interno.',
    distintivoImage: '/landing/distintivo-h.png',
    images: [
      { src: '/landing/higiene-seguridad-1.png', alt: 'Protocolos de higiene en cocina' },
      { src: '/landing/higiene-seguridad-2.png', alt: 'Control de calidad e inocuidad' },
      { src: '/landing/higiene-seguridad-3.png', alt: 'Supervisión de procesos alimentarios' },
    ],
  },

  filosofia: {
    quote:
      'Lograr tener clientes satisfechos es una tarea de equipo, trabajo constante, honesto y un compromiso inamovible con la calidad en todo sentido. Tratar a tus colaboradores como a tu familia, a tus proveedores como a tus amigos y a tus clientes como los invitados de honor definitivamente es una fórmula para el éxito en cualquier empresa.',
    author: 'Marcos F. Pérez Zúñiga',
    role: 'Director General',
    image: '/landing/marcos.png',
    imageAlt: 'Marcos F. Pérez Zúñiga, Director General',
  },

  clientes: {
    intro: 'Instituciones que confían en nuestro servicio de alimentación.',
    logos: [
      { name: 'Colegio México', src: '/landing/colegio-mexico.png' },
      { name: 'Ceneval', src: '/landing/ceneval.png' },
      { name: 'DGB', src: '/landing/dgb.png' },
      { name: 'Hospital Ángeles', src: '/landing/hospital-angeles.png' },
      { name: 'Grupo Cudec', src: '/landing/grupo-cudec.png' },
      { name: 'Consar', src: '/landing/consar.png' },
      { name: 'Posadas', src: '/landing/posadas.png' },
      { name: 'Bocanegra', src: '/landing/bocanegra.png' },
    ] satisfies LandingClientLogo[],
  },

  contacto: {
    headline: 'Hablemos de su próximo proyecto',
    intro:
      'Estamos listos para diseñar una solución a la medida de su institución. Contáctenos hoy mismo.',
    phone: '(+52) 55 3639 1856',
    email: 'perezmarcos25@hotmail.com',
    address:
      'Ahuejote 288, Colonia Pedregal de Santo Domingo, Ciudad de México, C.P. 04369',
  },

  footer: {
    tagline:
      'Servicio de alimentación institucional con calidez humana, higiene y excelencia desde 2010.',
  },
} as const;
