(function() {
  "use strict";

  /**
   * Apply .scrolled class to the body as the page is scrolled down
   */
  function toggleScrolled() {
    const selectBody = document.querySelector('body');
    const selectHeader = document.querySelector('#header');
    if (!selectHeader.classList.contains('scroll-up-sticky') && !selectHeader.classList.contains('sticky-top') && !selectHeader.classList.contains('fixed-top')) return;
    window.scrollY > 100 ? selectBody.classList.add('scrolled') : selectBody.classList.remove('scrolled');
  }

  document.addEventListener('scroll', toggleScrolled);
  window.addEventListener('load', toggleScrolled);

  /**
   * Mobile nav toggle
   */
  const mobileNavToggleBtn = document.querySelector('.mobile-nav-toggle');

  function mobileNavToogle() {
    document.querySelector('body').classList.toggle('mobile-nav-active');
    mobileNavToggleBtn.classList.toggle('bi-list');
    mobileNavToggleBtn.classList.toggle('bi-x');
  }
  if (mobileNavToggleBtn) {
    mobileNavToggleBtn.addEventListener('click', mobileNavToogle);
  }

  /**
   * Hide mobile nav on same-page/hash links
   */
  document.querySelectorAll('#navmenu a').forEach(navmenu => {
    navmenu.addEventListener('click', () => {
      if (document.querySelector('.mobile-nav-active')) {
        mobileNavToogle();
      }
    });
  });

  /**
   * Toggle mobile nav dropdowns
   */
  document.querySelectorAll('.navmenu .toggle-dropdown').forEach(navmenu => {
    navmenu.addEventListener('click', function(e) {
      e.preventDefault();
      this.parentNode.classList.toggle('active');
      this.parentNode.nextElementSibling.classList.toggle('dropdown-active');
      e.stopImmediatePropagation();
    });
  });

  /**
   * Preloader
   */
  const preloader = document.querySelector('#preloader');
  if (preloader) {
    window.addEventListener('load', () => {
      preloader.remove();
    });
  }

  /**
   * Scroll top button
   */
  let scrollTop = document.querySelector('.scroll-top');

  function toggleScrollTop() {
    if (scrollTop) {
      window.scrollY > 100 ? scrollTop.classList.add('active') : scrollTop.classList.remove('active');
    }
  }

  if (scrollTop) {
    scrollTop.addEventListener('click', (e) => {
      e.preventDefault();
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    });
  }

  window.addEventListener('load', toggleScrollTop);
  document.addEventListener('scroll', toggleScrollTop);

  /**
   * Animation on scroll function and init
   */
  function aosInit() {
    if (typeof AOS !== 'undefined') {
      AOS.init({
        duration: 600,
        easing: 'ease-in-out',
        once: true,
        mirror: false
      });
    }
  }
  window.addEventListener('load', aosInit);

  /**
   * Initiate glightbox
   */
  if (typeof GLightbox !== 'undefined') {
    const glightbox = GLightbox({
      selector: '.glightbox'
    });
  }

  /**
   * Skills animation
   */
  let skillsAnimation = document.querySelectorAll('.skills-animation');
  if (typeof Waypoint !== 'undefined') {
    skillsAnimation.forEach((item) => {
      new Waypoint({
        element: item,
        offset: '80%',
        handler: function(direction) {
          let progress = item.querySelectorAll('.progress .progress-bar');
          progress.forEach(el => {
            el.style.width = el.getAttribute('aria-valuenow') + '%';
          });
        }
      });
    });
  }

  /**
   * Initiate Pure Counter
   */
  if (typeof PureCounter !== 'undefined') {
    new PureCounter();
  }

  /**
   * Init swiper sliders
   */
  function initSwiper() {
    if (typeof Swiper !== 'undefined') {
      document.querySelectorAll(".init-swiper").forEach(function(swiperElement) {
        let config = JSON.parse(
          swiperElement.querySelector(".swiper-config").innerHTML.trim()
        );

        if (swiperElement.classList.contains("swiper-tab")) {
          initSwiperWithCustomPagination(swiperElement, config);
        } else {
          new Swiper(swiperElement, config);
        }
      });
    }
  }

  window.addEventListener("load", initSwiper);

  /**
   * Init isotope layout and filters
   */
  if (typeof Isotope !== 'undefined' && typeof imagesLoaded !== 'undefined') {
    document.querySelectorAll('.isotope-layout').forEach(function(isotopeItem) {
      let layout = isotopeItem.getAttribute('data-layout') ?? 'masonry';
      let filter = isotopeItem.getAttribute('data-default-filter') ?? '*';
      let sort = isotopeItem.getAttribute('data-sort') ?? 'original-order';

      let initIsotope;
      imagesLoaded(isotopeItem.querySelector('.isotope-container'), function() {
        initIsotope = new Isotope(isotopeItem.querySelector('.isotope-container'), {
          itemSelector: '.isotope-item',
          layoutMode: layout,
          filter: filter,
          sortBy: sort
        });
      });

      isotopeItem.querySelectorAll('.isotope-filters li').forEach(function(filters) {
        filters.addEventListener('click', function() {
          isotopeItem.querySelector('.isotope-filters .filter-active').classList.remove('filter-active');
          this.classList.add('filter-active');
          initIsotope.arrange({
            filter: this.getAttribute('data-filter')
          });
          if (typeof aosInit === 'function') {
            aosInit();
          }
        }, false);
      });
    });
  }

  /**
   * Frequently Asked Questions Toggle
   */
  document.querySelectorAll('.faq-item h3, .faq-item .faq-toggle').forEach((faqItem) => {
    faqItem.addEventListener('click', () => {
      faqItem.parentNode.classList.toggle('faq-active');
    });
  });

  /**
   * Correct scrolling position upon page load for URLs containing hash links.
   */
  window.addEventListener('load', function(e) {
    if (window.location.hash) {
      if (document.querySelector(window.location.hash)) {
        setTimeout(() => {
          let section = document.querySelector(window.location.hash);
          let scrollMarginTop = getComputedStyle(section).scrollMarginTop;
          window.scrollTo({
            top: section.offsetTop - parseInt(scrollMarginTop),
            behavior: 'smooth'
          });
        }, 100);
      }
    }
  });

  /**
   * Navmenu Scrollspy
   */
  let navmenulinks = document.querySelectorAll('.navmenu a');

  function navmenuScrollspy() {
    navmenulinks.forEach(navmenulink => {
      if (!navmenulink.hash) return;
      let section = document.querySelector(navmenulink.hash);
      if (!section) return;
      let position = window.scrollY + 200;
      if (position >= section.offsetTop && position <= (section.offsetTop + section.offsetHeight)) {
        document.querySelectorAll('.navmenu a.active').forEach(link => link.classList.remove('active'));
        navmenulink.classList.add('active');
      } else {
        navmenulink.classList.remove('active');
      }
    })
  }
  window.addEventListener('load', navmenuScrollspy);
  document.addEventListener('scroll', navmenuScrollspy);

  /**
   * Sticky header with hide on scroll down
   */
  const selectHeader = document.querySelector('#header');
  if (selectHeader) {
    const floatingContactBtn = document.getElementById('openContactPanel');
    let lastScrollY = window.scrollY;

    const handleHeaderScroll = () => {
      const currentY = window.scrollY;
      const topbar = selectHeader.querySelector('.topbar');

      // Add sticked class
      if (currentY > 100) {
        selectHeader.classList.add('sticked');
      } else {
        selectHeader.classList.remove('sticked');
      }

      // Handle navigation hide/show on desktop
      if (window.innerWidth >= 992) {
        if (currentY < 80) {
          selectHeader.classList.remove('nav-hidden');
        } else if (currentY > lastScrollY + 6) {
          selectHeader.classList.add('nav-hidden');
        } else if (currentY < lastScrollY - 6) {
          selectHeader.classList.remove('nav-hidden');
        }
      } else {
        selectHeader.classList.remove('nav-hidden');
      }

      // Handle floating contact button
      if (floatingContactBtn) {
        if (currentY > 400) {
          floatingContactBtn.classList.remove('is-hidden');
        } else {
          floatingContactBtn.classList.add('is-hidden');
        }
      }

      lastScrollY = currentY;
    };

    window.addEventListener('scroll', handleHeaderScroll);
    window.addEventListener('load', handleHeaderScroll);
    window.addEventListener('resize', handleHeaderScroll);
    handleHeaderScroll();
  }

  /**
   * Dropdown toggle
   */
  document.querySelectorAll('.navmenu .dropdown > a').forEach(link => {
    link.addEventListener('click', function (e) {
      if (document.querySelector('.mobile-nav-active')) {
        e.preventDefault();
        const parentLi = this.closest('li');
        const dropdown = parentLi?.querySelector('ul');
        this.classList.toggle('active');
        parentLi?.classList.toggle('active');
        dropdown?.classList.toggle('dropdown-active');
      }
    });
  });

  /**
   * Smooth scroll for same-page nav links
   */
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    const href = anchor.getAttribute('href');
    if (!href || href === '#' || href === '#!' || href === '#0') {
      return;
    }

    anchor.addEventListener('click', function (e) {
      const target = document.querySelector(href);
      if (target) {
        e.preventDefault();
        target.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
        if (document.querySelector('.mobile-nav-active') && mobileNavToggleBtn) {
          document.querySelector('body').classList.remove('mobile-nav-active');
          mobileNavToggleBtn.classList.add('bi-list');
          mobileNavToggleBtn.classList.remove('bi-x');
        }
      }
    });
  });

})();

// ============================================================================
// LEAD FORM SUBMISSION HANDLER WITH SWEETALERT2
// ============================================================================
(function() {
    'use strict';

    // Check if SweetAlert2 is loaded
    if (typeof Swal === 'undefined') {
        console.error('SweetAlert2 is not loaded! Please add the library.');
        return;
    }

    /**
     * Submit lead data to API
     */
    async function submitLead(formData) {
        try {
            const response = await fetch('/api/leads', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            const result = await response.json();

            if (response.ok && result.success) {
                return { success: true, message: result.message };
            } else {
                // Handle validation errors
                let errorMessages = [];

                if (result.errors && typeof result.errors === 'object') {
                    // Validation errors from backend
                    for (const [field, message] of Object.entries(result.errors)) {
                        errorMessages.push(`<strong>${field}:</strong> ${message}`);
                    }
                } else if (result.message) {
                    errorMessages.push(result.message);
                }

                return {
                    success: false,
                    message: errorMessages.length > 0
                        ? errorMessages.join('<br>')
                        : 'Failed to submit. Please try again.'
                };
            }
        } catch (error) {
            console.error('Error submitting form:', error);
            return {
                success: false,
                message: 'Network error. Please check your connection and try again.'
            };
        }
    }

    /**
     * Main Contact Modal Form Handler
     */
    const contactForm = document.getElementById('contactForm');
    const contactModalEl = document.getElementById('contactModal');

    if (contactForm) {
        contactForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            // Get form data with proper IDs
            const nameInput = document.getElementById('name');
            const mobileInput = document.getElementById('mobile');
            const emailInput = document.getElementById('email');
            const serviceSelect = document.getElementById('service');
            const messageTextarea = document.getElementById('message');

            // Validate elements exist
            if (!nameInput || !mobileInput || !emailInput) {
                Swal.fire({
                    icon: 'error',
                    title: 'Form Error',
                    text: 'Required form fields are missing. Please refresh the page.',
                    confirmButtonColor: '#667eea'
                });
                return;
            }

            const formData = {
                name: nameInput.value.trim(),
                mobile: mobileInput.value.trim(),
                email: emailInput.value.trim(),
                service: serviceSelect ? serviceSelect.value : '',
                message: messageTextarea ? messageTextarea.value.trim() : ''
            };

            // Client-side validation
            if (!formData.name || !formData.mobile || !formData.email) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Missing Information',
                    text: 'Please fill in all required fields (Name, Mobile, Email)',
                    confirmButtonColor: '#667eea'
                });
                return;
            }

            // Show loading
            Swal.fire({
                title: 'Submitting...',
                html: 'Please wait while we process your request',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Submit to API
            const result = await submitLead(formData);

            if (result.success) {
                // Success notification
                Swal.fire({
                    icon: 'success',
                    title: 'Thank You!',
                    html: 'We have received your inquiry.<br>Our team will contact you soon.',
                    confirmButtonColor: '#667eea',
                    confirmButtonText: 'Great!'
                });

                // Reset form
                contactForm.reset();

                // Close modal after delay
                if (typeof bootstrap !== 'undefined' && contactModalEl) {
                    const modalInstance = bootstrap.Modal.getInstance(contactModalEl);
                    if (modalInstance) {
                        setTimeout(() => modalInstance.hide(), 1500);
                    }
                }
            } else {
                // Error notification
                Swal.fire({
                    icon: 'error',
                    title: 'Submission Failed',
                    html: result.message,
                    confirmButtonColor: '#667eea',
                    confirmButtonText: 'Try Again'
                });
            }
        });
    }

    /**
     * Hero Contact Form - Pre-fill modal
     */
    const heroContactForm = document.getElementById('heroContactForm');
    if (heroContactForm) {
        const modalTriggerBtn = heroContactForm.querySelector('[data-bs-target="#contactModal"]');

        if (modalTriggerBtn) {
            modalTriggerBtn.addEventListener('click', () => {
                // Pre-fill name from hero form if available
                const heroNameInput = heroContactForm.querySelector('input[type="text"]');
                const heroName = heroNameInput?.value.trim() || '';

                if (heroName && contactForm) {
                    const modalNameInput = contactForm.querySelector('#name');
                    if (modalNameInput) {
                        modalNameInput.value = heroName;
                    }
                }
            });
        }
    }

    /**
     * Bottom Contact Section Form Handler
     * IMPORTANT: This form uses 'subject-field' which maps to 'service' in the backend
     */
    const bottomContactForm = document.querySelector('.php-email-form');
    if (bottomContactForm) {
        // Remove the onclick alert from button
        const submitBtn = bottomContactForm.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.removeAttribute('onclick');
        }

        // Add proper form submission handler
        bottomContactForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            // Get form data from bottom form - NOTE: subject-field maps to service
            const nameField = bottomContactForm.querySelector('#name-field');
            const emailField = bottomContactForm.querySelector('#email-field');
            const mobileField = bottomContactForm.querySelector('#mobile-field');
            const subjectField = bottomContactForm.querySelector('#subject-field'); // This is the SERVICE
            const messageField = bottomContactForm.querySelector('#message-field');

            // Validate elements exist
            if (!nameField || !emailField || !mobileField) {
                Swal.fire({
                    icon: 'error',
                    title: 'Form Error',
                    text: 'Required form fields are missing. Please refresh the page.',
                    confirmButtonColor: '#667eea'
                });
                return;
            }

            const formData = {
                name: nameField.value.trim(),
                email: emailField.value.trim(),
                mobile: mobileField.value.trim(),
                service: subjectField ? subjectField.value.trim() : '', // Subject = Service
                message: messageField ? messageField.value.trim() : ''
            };

            // Client-side validation
            if (!formData.name || !formData.email || !formData.mobile) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Missing Information',
                    text: 'Please fill in all required fields (Name, Email, Mobile)',
                    confirmButtonColor: '#667eea'
                });
                return;
            }

            // Show loading
            Swal.fire({
                title: 'Sending Message...',
                html: 'Please wait',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Submit to API
            const result = await submitLead(formData);

            if (result.success) {
                Swal.fire({
                    icon: 'success',
                    title: 'Message Sent!',
                    text: 'Thank you for contacting us. We will get back to you soon.',
                    confirmButtonColor: '#667eea'
                });

                // Reset form
                bottomContactForm.reset();
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Failed to Send',
                    html: result.message,
                    confirmButtonColor: '#667eea'
                });
            }
        });
    }

})();