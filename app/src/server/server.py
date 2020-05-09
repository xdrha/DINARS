import flask
import os
import os.path
import io
from flask import jsonify
from flask import request, Response
from flask import send_file
import matplotlib.pyplot as plt
from matplotlib.figure import Figure
from pylab import *
import cv2
import matplotlib.dates as md
import datetime as dt


app = flask.Flask(__name__)
matplotlib.rc('axes',edgecolor='white')

fig = plt.figure(figsize=(8,4.5))

@app.route('/upload_data', methods=['POST'])
def upload():
    try:        
        globalD = request.json['globalDistraction']
        phoneD = request.json['phoneDistraction']
        coffeeD = request.json['coffeeDistraction']
        drowsiness = request.json['drowsinessLevel']

        f = open('statistics.txt', 'a')
        f.write(str(md.date2num(dt.datetime.now())) + " " + str(globalD) + " " + str(phoneD) + " " + str(coffeeD) + " " + str(drowsiness) + "\n")
        f.close() 

    except Exception as e:
        return jsonify({'result': "ERROR"}), 400

    return jsonify({'result': 'OK'}), 200


@app.route('/clear_stats', methods=['GET'])
def clear_stats():
    try:
        if(os.path.isfile('overall.png')):
            os.remove('overall.png')
        if(os.path.isfile('phone.png')):
            os.remove('phone.png')
        if(os.path.isfile('coffee.png')):
            os.remove('coffee.png')
        if(os.path.isfile('drowsiness.png')):    
            os.remove('drowsiness.png')
        if(os.path.isfile('statistics.txt')):
            open('statistics.txt', 'w').close()
    except Exception as e:
        return jsonify({'result': "ERROR"}), 400

    return jsonify({'result': 'OK'}), 200


@app.route('/load_graph_data', methods=['GET'])
def load_graph_data():

    avgGlobal = 0.0
    avgPhone = 0.0
    avgCoffee = 0.0
    avgDrowsiness = 0.0

    timestampArray = []
    globalDistractionArray = []
    phoneDistractionArray = []
    coffeeDistractionArray = []
    drowsinessLevelArray = []
    xAxis = []

    with open('statistics.txt') as f:

        line = f.readline()
        cnt = 0
        while line:
            values = line.split(' ')
            
            timestampArray.append(float(values[0]))
            globalDistractionArray.append(float(values[1]))
            phoneDistractionArray.append(float(values[2])*2)
            coffeeDistractionArray.append(float(values[3])*2)
            drowsinessLevelArray.append(float(values[4])/2)
            xAxis.append(cnt)

            line = f.readline()
            cnt += 1

    f.close()

    dates=[dt.datetime.fromtimestamp(ts) for ts in timestampArray]

    if(cnt > 500):
        valueCount = (cnt / 100) + 1

        globalDistractionArrayScaled = []
        phoneDistractionArrayScaled = []
        coffeeDistractionArrayScaled = []
        drowsinessLevelArrayScaled = []
        timestampArrayScaled = []
        xAxisScaled = []
        summ = 0
        counter = 0
        newSumm = 0
        
        for e in globalDistractionArray:
            summ += e
            counter += 1
            if counter == valueCount:
                globalDistractionArrayScaled.append(summ / valueCount)
                newSumm += 1
                xAxisScaled.append(newSumm)
                counter = 0
                summ = 0

        if counter > 0:
            globalDistractionArrayScaled.append(summ / valueCount)
            newSumm += 1
            xAxisScaled.append(newSumm)
            counter = 0
            summ = 0

        for e in phoneDistractionArray:
            summ += e
            counter += 1
            if counter == valueCount:
                phoneDistractionArrayScaled.append(summ / valueCount)
                counter = 0
                summ = 0

        if counter > 0:
            phoneDistractionArrayScaled.append(summ / valueCount)
            counter = 0
            summ = 0

        for e in coffeeDistractionArray:
            summ += e
            counter += 1
            if counter == valueCount:
                coffeeDistractionArrayScaled.append(summ / valueCount)
                counter = 0
                summ = 0

        if counter > 0:
            coffeeDistractionArrayScaled.append(summ / valueCount)
            counter = 0
            summ = 0

        for e in drowsinessLevelArray:
            summ += e
            counter += 1
            if counter == valueCount:
                drowsinessLevelArrayScaled.append(summ / valueCount)
                counter = 0
                summ = 0

        if counter > 0:
            drowsinessLevelArrayScaled.append(summ / valueCount)
            counter = 0
            summ = 0

        for e in timestampArray:
            summ += e
            counter += 1
            if counter == valueCount:
                timestampArrayScaled.append(summ / valueCount)
                counter = 0
                summ = 0

        if counter > 0:
            timestampArrayScaled.append(summ / valueCount)
            counter = 0
            summ = 0

        avgGlobal = round(Average(globalDistractionArrayScaled), 2)
        avgPhone = round(Average(phoneDistractionArrayScaled), 2)
        avgCoffee = round(Average(coffeeDistractionArrayScaled), 2)
        avgDrowsiness = round(Average(drowsinessLevelArrayScaled), 2)

        draw_graph(globalDistractionArrayScaled, timestampArrayScaled, "DISTRACTION", "Overall distraction graph", "overall.png", avgGlobal)
        draw_graph(phoneDistractionArrayScaled, timestampArrayScaled, "PHONE USAGE", "Phone usage graph", "phone.png", avgPhone)
        draw_graph(coffeeDistractionArrayScaled, timestampArrayScaled, "COFFEE HOLDING", "Coffee holding graph", "coffee.png", avgCoffee)
        draw_graph(drowsinessLevelArrayScaled, timestampArrayScaled, "DROWSINESS LEVEL", "Drowsiness level graph", "drowsiness.png", avgDrowsiness)
    else:
        avgGlobal = round(Average(globalDistractionArray), 2)
        avgPhone = round(Average(phoneDistractionArray), 2)
        avgCoffee = round(Average(coffeeDistractionArray), 2)
        avgDrowsiness = round(Average(drowsinessLevelArray), 2)   

        draw_graph(globalDistractionArray, timestampArray, "DISTRACTION", "Overall distraction graph", "overall.png", avgGlobal)
        draw_graph(phoneDistractionArray, timestampArray, "PHONE USAGE", "Phone usage graph", "phone.png", avgPhone)
        draw_graph(coffeeDistractionArray, timestampArray, "COFFEE HOLDING", "Coffee holding graph", "coffee.png", avgCoffee)
        draw_graph(drowsinessLevelArray, timestampArray, "DROWSINESS LEVEL", "Drowsiness level graph", "drowsiness.png", avgDrowsiness)

    return jsonify({'avg_overall': str(avgGlobal),
                    'avg_phone': str(avgPhone),
                    'avg_coffee': str(avgCoffee),
                    'avg_drowsiness': str(avgDrowsiness)}), 200


def draw_graph(yAxis, xAxis, ylabel, title, filename, averageRate):

    global fig

    if(averageRate >= 2):
        if(averageRate < 5):
            color = "yellow"
        else: color = "red"
    else: color = "green"

    fig.patch.set_facecolor('#0a0a0a')
    ax = fig.add_subplot(111, facecolor='#0a0a0a')
    ax.tick_params(axis='x', colors='white')
    ax.tick_params(axis='y', colors='white')

    xfmt = md.DateFormatter('%H:%M:%S')
    plt.xticks( rotation=0 )
    ax.xaxis.set_major_formatter(xfmt)

    plt.axhline(y=2, color='green', linewidth=0.5, linestyle='--')
    plt.axhline(y=5, color='yellow', linewidth=0.5, linestyle='--')
    plt.xlabel('TIME', color='white')
    plt.ylim(0, 11)
    
    plt.plot(xAxis[0:len(xAxis) -1 ], yAxis[0:len(yAxis) - 1], linewidth=2, color=color)
    plt.ylabel(ylabel, color='white')
    #plt.title(title, color='white')
    plt.savefig(filename, facecolor=fig.get_facecolor(), edgecolor='none')

    plt.clf()


def Average(lst): 
    return sum(lst) / len(lst)


@app.route('/get_graph', methods=['GET'])
def get_graph():
    graph_type = request.args.get('type')
    return send_file(graph_type + '.png', mimetype='image/png')     


@app.route('/check_availability', methods=['GET'])
def check_availability():
    return flask.jsonify({'response': 'OK'}), 200  



def generate():
    while True:
        video = cv2.VideoCapture('test_velky_0.mp4')
        while True:
            ret, frame = video.read()
            time.sleep(0.2)
            if frame is not None:
                (flag, encodedImage) = cv2.imencode(".jpg", frame)

                if flag:
                    yield b'--frame\r\n' 
                    yield b'Content-Type: image/jpeg\r\n'
                    yield b'Content-Length: ' + str(len(encodedImage)) + b'\r\n\r\n'
                    yield encodedImage.tobytes() + b'\r\n'
            else: 
                break


@app.route("/video_feed", methods=['GET'])
def video_feed():
   return Response(generate(), mimetype = "multipart/x-mixed-replace; boundary=--frame")


if __name__ == '__main__':
    app.run(host='0.0.0.0')