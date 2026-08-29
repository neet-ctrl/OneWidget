import{j as e}from"./index-BoYU7UgY.js";function t(){return e.jsxs("main",{className:"mockup",children:[e.jsxs("div",{className:"widget-stage","aria-label":"Sakura clock and weather widget",children:[e.jsx("img",{className:"widget-art",src:"/__widget-mockup/images/sakura-empty-template.jpg",alt:"Sakura widget artwork with empty clock, date, and weather text areas"}),e.jsxs("div",{className:"clock","aria-label":"07:40 PM",children:[e.jsx("span",{className:"clock-time",children:"07:40"}),e.jsx("span",{className:"clock-meridiem",children:"PM"})]}),e.jsxs("div",{className:"date","aria-label":"Sunday, 18 May",children:[e.jsx("span",{className:"calendar-icon","aria-hidden":"true"}),e.jsx("span",{children:"Sunday, 18 May"})]}),e.jsxs("div",{className:"weather","aria-label":"34 degrees, Cloudy",children:[e.jsx("span",{className:"temperature",children:"34°"}),e.jsx("span",{className:"condition",children:"Cloudy"})]})]}),e.jsx("style",{children:`
        :root {
          color-scheme: dark;
        }

        * {
          box-sizing: border-box;
        }

        html,
        body,
        #root {
          width: 100%;
          min-height: 100%;
          margin: 0;
        }

        body {
          overflow: hidden;
          background: #000;
        }

        .mockup {
          width: 100vw;
          min-height: 100vh;
          display: grid;
          place-items: center;
          background: #000;
        }

        .widget-stage {
          position: relative;
          width: min(100vw, calc(100vh * 2.273));
          aspect-ratio: 1280 / 563;
          container-type: inline-size;
          overflow: hidden;
        }

        .widget-art {
          position: absolute;
          inset: 0;
          width: 100%;
          height: 100%;
          display: block;
          object-fit: fill;
          user-select: none;
          -webkit-user-drag: none;
        }

        .clock {
          position: absolute;
          top: 28%;
          left: 23.5%;
          width: 44.5%;
          height: 26%;
          display: flex;
          align-items: flex-end;
          justify-content: center;
          color: #ca6d88;
          white-space: nowrap;
          text-shadow: 0 0.15cqw 0.22cqw rgba(255, 255, 255, .5);
        }

        .clock-time {
          font-family: Arial, Helvetica, sans-serif;
          font-size: 11.7cqw;
          font-weight: 400;
          line-height: .82;
          letter-spacing: .01em;
        }

        .clock-meridiem {
          align-self: flex-end;
          margin: 0 0 1.5cqw .6cqw;
          font-family: Arial, Helvetica, sans-serif;
          font-size: 3.05cqw;
          line-height: 1;
          font-weight: 500;
        }

        .date {
          position: absolute;
          top: 75.8%;
          left: 30.6%;
          width: 33.3%;
          height: 10.6%;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 1.3cqw;
          color: #b2697e;
          font-family: Arial, Helvetica, sans-serif;
          font-size: 2.25cqw;
          font-weight: 500;
          line-height: 1;
          white-space: nowrap;
        }

        .calendar-icon {
          position: relative;
          width: 1.55cqw;
          height: 1.55cqw;
          flex: 0 0 auto;
          border: .2cqw solid #b2697e;
          border-radius: .28cqw;
        }

        .calendar-icon::before {
          content: "";
          position: absolute;
          top: .35cqw;
          left: -.2cqw;
          width: calc(100% + .4cqw);
          border-top: .2cqw solid #b2697e;
        }

        .calendar-icon::after {
          content: "··";
          position: absolute;
          left: .18cqw;
          top: .52cqw;
          font-size: 1.1cqw;
          letter-spacing: .28cqw;
          line-height: .5;
        }

        .weather {
          position: absolute;
          top: 69%;
          left: 82.8%;
          width: 14.3%;
          color: #3e4147;
          font-family: Arial, Helvetica, sans-serif;
          text-align: left;
          white-space: nowrap;
        }

        .temperature {
          display: block;
          font-size: 5.5cqw;
          font-weight: 700;
          line-height: .95;
        }

        .condition {
          display: block;
          margin-top: .75cqw;
          font-size: 2.25cqw;
          font-weight: 400;
          line-height: 1;
        }
      `})]})}export{t as SakuraWidgetPosition};
