import flask
import os
import io
from flask import jsonify
from flask import request
from flask import send_file
import matplotlib.pyplot as plt
from matplotlib.figure import Figure
from pylab import *

app = flask.Flask(__name__)
matplotlib.rc('axes',edgecolor='white')

xAxis = []
fig = plt.figure(figsize=(8,4.5))

@app.route('/upload_data', methods=['POST'])
def upload():
    try:        
        timestamp = request.json['timestamp']
        globalD = request.json['globalDistraction']
        phoneD = request.json['phoneDistraction']
        coffeeD = request.json['coffeeDistraction']
        drowsiness = request.json['drowsinessLevel']

        f = open('statistics.txt', 'a')
        f.write(str(timestamp) + " " + str(globalD) + " " + str(phoneD) + " " + str(coffeeD) + " " + str(drowsiness) + "\n")
        f.close() 

    except Exception as e:
        return jsonify({'result': "ERROR"}), 400

    return jsonify({'result': 'OK'}), 200


@app.route('/clear_stats', methods=['GET'])
def clear_stats():
    try:
        os.remove('overall.png')
        os.remove('phone.png')
        os.remove('coffee.png')
        os.remove('drowsiness.png')
        open('statistics.txt', 'w').close()
    except Exception as e:
        return jsonify({'result': "ERROR"}), 400

    return jsonify({'result': 'OK'}), 200


@app.route('/load_graph_data', methods=['GET'])
def load_graph_data():

    global xAxis

    timestamps = []
    globalDs = []
    phoneDs = []
    coffeeDs = []
    drowsinesss = []
    xAxis = []

    with open('statistics.txt') as f:

        line = f.readline()
        cnt = 0
        while line:
            values = line.split(' ')
            
            timestamps.append(values[0])
            globalDs.append(float(values[1]))
            phoneDs.append(float(values[2])*2)
            coffeeDs.append(float(values[3])*2)
            drowsinesss.append(float(values[4])/2)
            xAxis.append(cnt)

            line = f.readline()
            cnt += 1

    f.close()

    draw_graph(globalDs, "DISTRACTION", "Overall distraction graph", "overall.png", round(Average(globalDs), 2))
    draw_graph(phoneDs, "PHONE USAGE", "Phone usage graph", "phone.png", round(Average(phoneDs), 2))
    draw_graph(coffeeDs, "COFFEE HOLDING", "Coffee holding graph", "coffee.png", round(Average(coffeeDs), 2))
    draw_graph(drowsinesss, "DROWSINESS LEVEL", "Drowsiness level graph", "drowsiness.png", round(Average(drowsinesss), 2))

    return jsonify({'avg_overall': str(round(Average(globalDs), 2)),
                    'avg_phone': str(round(Average(phoneDs), 2)),
                    'avg_coffee': str(round(Average(coffeeDs), 2)),
                    'avg_drowsiness': str(round(Average(drowsinesss), 2))}), 200


def draw_graph(yAxis, ylabel, title, filename, averageRate):

    global timestamps, globalDs, phoneDs, coffeeDs, drowsinesss, xAxis, fig

    if(averageRate >= 2):
        if(averageRate < 5):
            color = "yellow"
        else: color = "red"
    else: color = "green"

    fig.patch.set_facecolor('#0a0a0a')
    ax = fig.add_subplot(111, facecolor='#0a0a0a')
    ax.tick_params(axis='x', colors='white')
    ax.tick_params(axis='y', colors='white')
    plt.axhline(y=2, color='green', linewidth=0.5, linestyle='--')
    plt.axhline(y=5, color='yellow', linewidth=0.5, linestyle='--')
    plt.xlabel('TIME (s)', color='white')
    plt.ylim(0, 11)
    
    plt.plot(xAxis[0:len(xAxis) -1 ], yAxis[0:len(yAxis) - 1], linewidth=2, color=color)
    plt.ylabel(ylabel, color='white')
    plt.title(title, color='white')
    plt.savefig(filename, facecolor=fig.get_facecolor(), edgecolor='none')

    plt.clf()
    print("ok 4")


def Average(lst): 
    return sum(lst) / len(lst)


@app.route('/get_graph', methods=['GET'])
def get_graph():

    graph_type = request.args.get('type')
    return send_file(graph_type + '.png', mimetype='image/png')     


@app.route('/check_availability', methods=['GET'])
def check_availability():
    return flask.jsonify({'response': 'OK'}), 200  


if __name__ == '__main__':
    app.run(host='0.0.0.0')