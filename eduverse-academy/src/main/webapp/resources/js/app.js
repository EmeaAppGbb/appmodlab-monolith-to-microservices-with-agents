/* ============================================================
   EduVerse Academy – Application JavaScript
   Requires jQuery (loaded via Bootstrap 4 dependency)
   ============================================================ */

(function ($) {
    'use strict';

    /* ----------------------------------------------------------
       1. Video Player Controls
    ---------------------------------------------------------- */
    var EduPlayer = {
        init: function () {
            var $video = $('#eduverse-video');
            if (!$video.length) return;

            this.video = $video[0];
            this.$container = $video.closest('.video-player-wrapper');
            this.$controls = this.$container.siblings('.video-controls');

            this.bindEvents();
        },

        bindEvents: function () {
            var self = this;

            this.$controls.on('click', '[data-action="play-pause"]', function () {
                self.togglePlay();
            });

            this.$controls.on('click', '[data-action="mute"]', function () {
                self.toggleMute($(this));
            });

            this.$controls.on('input', '[data-action="seek"]', function () {
                self.seek($(this).val());
            });

            this.$controls.on('input', '[data-action="volume"]', function () {
                self.setVolume($(this).val());
            });

            this.$controls.on('click', '[data-action="fullscreen"]', function () {
                self.toggleFullscreen();
            });

            $(this.video).on('timeupdate', function () {
                self.updateProgress();
            });

            $(this.video).on('ended', function () {
                self.onEnded();
            });

            $(this.video).on('loadedmetadata', function () {
                self.updateDuration();
            });
        },

        togglePlay: function () {
            var $btn = this.$controls.find('[data-action="play-pause"] i');
            if (this.video.paused) {
                this.video.play();
                $btn.removeClass('fa-play').addClass('fa-pause');
            } else {
                this.video.pause();
                $btn.removeClass('fa-pause').addClass('fa-play');
            }
        },

        toggleMute: function ($btn) {
            this.video.muted = !this.video.muted;
            var $icon = $btn.find('i');
            $icon.toggleClass('fa-volume-up fa-volume-mute');
        },

        seek: function (value) {
            if (this.video.duration) {
                this.video.currentTime = (value / 100) * this.video.duration;
            }
        },

        setVolume: function (value) {
            this.video.volume = value / 100;
        },

        updateProgress: function () {
            if (!this.video.duration) return;
            var pct = (this.video.currentTime / this.video.duration) * 100;
            this.$controls.find('[data-action="seek"]').val(pct);
            this.$controls.find('.current-time').text(this.formatTime(this.video.currentTime));
        },

        updateDuration: function () {
            this.$controls.find('.total-time').text(this.formatTime(this.video.duration));
        },

        onEnded: function () {
            this.$controls.find('[data-action="play-pause"] i')
                .removeClass('fa-pause').addClass('fa-play');
            this.markLessonComplete();
        },

        markLessonComplete: function () {
            var lessonId = this.$container.data('lesson-id');
            var enrollmentId = this.$container.data('enrollment-id');
            if (!lessonId || !enrollmentId) return;

            $.post('/progress/complete', {
                lessonId: lessonId,
                enrollmentId: enrollmentId
            }).done(function () {
                $('.lesson-status[data-lesson="' + lessonId + ']')
                    .html('<i class="fas fa-check-circle text-success"></i>');
            });
        },

        toggleFullscreen: function () {
            var el = this.$container[0];
            if (!document.fullscreenElement) {
                (el.requestFullscreen || el.webkitRequestFullscreen || el.msRequestFullscreen).call(el);
            } else {
                (document.exitFullscreen || document.webkitExitFullscreen || document.msExitFullscreen).call(document);
            }
        },

        formatTime: function (seconds) {
            if (isNaN(seconds)) return '0:00';
            var m = Math.floor(seconds / 60);
            var s = Math.floor(seconds % 60);
            return m + ':' + (s < 10 ? '0' : '') + s;
        }
    };

    /* ----------------------------------------------------------
       2. Assessment / Quiz Submission
    ---------------------------------------------------------- */
    var Assessment = {
        init: function () {
            this.$form = $('#assessment-form');
            if (!this.$form.length) return;

            this.bindEvents();
            this.startTimer();
        },

        bindEvents: function () {
            var self = this;

            this.$form.on('submit', function (e) {
                e.preventDefault();
                self.submit();
            });
        },

        startTimer: function () {
            var $timer = $('#assessment-timer');
            if (!$timer.length) return;

            var remaining = parseInt($timer.data('seconds'), 10);
            if (isNaN(remaining) || remaining <= 0) return;

            this.timerInterval = setInterval(function () {
                remaining--;
                var mins = Math.floor(remaining / 60);
                var secs = remaining % 60;
                $timer.find('.timer-value').text(mins + ':' + (secs < 10 ? '0' : '') + secs);

                if (remaining <= 60) {
                    $timer.addClass('text-danger');
                }
                if (remaining <= 0) {
                    clearInterval(this.timerInterval);
                    Assessment.submit();
                }
            }.bind(this), 1000);
        },

        submit: function () {
            if (this.timerInterval) clearInterval(this.timerInterval);

            var answers = {};
            this.$form.find('.assessment-question').each(function () {
                var qId = $(this).data('question-id');
                var checked = $(this).find('input:checked');
                var textVal = $(this).find('textarea').val();
                if (checked.length) {
                    answers[qId] = checked.val();
                } else if (textVal) {
                    answers[qId] = textVal;
                }
            });

            var unanswered = this.$form.find('.assessment-question').length - Object.keys(answers).length;
            if (unanswered > 0 && !confirm('You have ' + unanswered + ' unanswered question(s). Submit anyway?')) {
                return;
            }

            var assessmentId = this.$form.data('assessment-id');
            $.ajax({
                url: '/assessments/' + assessmentId + '/submit',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ answers: answers }),
                beforeSend: function () {
                    $('#submit-assessment').prop('disabled', true).text('Submitting…');
                },
                success: function (response) {
                    if (response.redirectUrl) {
                        window.location.href = response.redirectUrl;
                    } else {
                        window.location.reload();
                    }
                },
                error: function () {
                    alert('Submission failed. Please try again.');
                    $('#submit-assessment').prop('disabled', false).text('Submit');
                }
            });
        }
    };

    /* ----------------------------------------------------------
       3. Form Validation Helpers
    ---------------------------------------------------------- */
    var FormValidator = {
        init: function () {
            this.bindAll();
        },

        bindAll: function () {
            $('form[data-validate]').each(function () {
                $(this).on('submit', function (e) {
                    if (!FormValidator.validate($(this))) {
                        e.preventDefault();
                        e.stopPropagation();
                    }
                    $(this).addClass('was-validated');
                });
            });

            // Live feedback on fields
            $('form[data-validate] input, form[data-validate] textarea, form[data-validate] select')
                .on('blur', function () {
                    FormValidator.validateField($(this));
                });
        },

        validate: function ($form) {
            var valid = true;
            $form.find('[required]').each(function () {
                if (!FormValidator.validateField($(this))) {
                    valid = false;
                }
            });
            return valid;
        },

        validateField: function ($field) {
            var value = $field.val();
            var valid = true;

            // Required check
            if ($field.prop('required') && (!value || !value.trim())) {
                valid = false;
            }

            // Email pattern
            if (valid && $field.attr('type') === 'email' && value) {
                valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
            }

            // Min-length
            var minLen = $field.attr('minlength');
            if (valid && minLen && value && value.length < parseInt(minLen, 10)) {
                valid = false;
            }

            // Price (non-negative number)
            if (valid && $field.data('validate-price')) {
                valid = !isNaN(value) && parseFloat(value) >= 0;
            }

            $field.toggleClass('is-invalid', !valid).toggleClass('is-valid', valid);
            return valid;
        }
    };

    /* ----------------------------------------------------------
       4. Miscellaneous UI Helpers
    ---------------------------------------------------------- */
    function initTooltips() {
        $('[data-toggle="tooltip"]').tooltip();
    }

    function initConfirmDialogs() {
        $('[data-confirm]').on('click', function (e) {
            if (!confirm($(this).data('confirm'))) {
                e.preventDefault();
            }
        });
    }

    function initProgressAnimations() {
        $('.progress-bar').each(function () {
            var target = $(this).attr('aria-valuenow') || $(this).data('target');
            if (target) {
                $(this).css('width', '0%').animate({ width: target + '%' }, 800);
            }
        });
    }

    /* ----------------------------------------------------------
       5. Bootstrap on DOM Ready
    ---------------------------------------------------------- */
    $(document).ready(function () {
        EduPlayer.init();
        Assessment.init();
        FormValidator.init();
        initTooltips();
        initConfirmDialogs();
        initProgressAnimations();
    });

})(jQuery);
